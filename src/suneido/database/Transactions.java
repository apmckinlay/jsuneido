/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.util.Verify.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import suneido.language.builtin.SuTransaction;
import suneido.util.ByteBuf;
import suneido.util.Print;

import com.google.common.collect.Sets;

/**
 * Manages transactions. Uses {@link Locks}
 */
// NOTE: don't call any Transaction synchronized methods while synchronized
// because this can lead to deadlock.
@ThreadSafe
class Transactions {
	private final Database db;
	private final AtomicLong clock = new AtomicLong();
	private final AtomicInteger nextNum = new AtomicInteger();
	private final Locks locks = new Locks();
	/** all active transactions */
	private final Set<Transaction> trans = Sets.newHashSet();
	/** active read-write transactions */
	private final PriorityQueue<Transaction> rwtrans =
			new PriorityQueue<Transaction>(MAX_FINALS_SIZE, Transaction.byAsof);
	/** committed read-write transactions waiting to be finalized */
	private final PriorityQueue<Transaction> finals =
			new PriorityQueue<Transaction>(MAX_FINALS_SIZE, Transaction.byCommit);
	private static final long FUTURE = Long.MAX_VALUE;
	// only overridden by tests, otherwise could be private final
	static int MAX_SHADOWS_SINCE_ACTIVITY = 1000;
	static int MAX_FINALS_SIZE = 200;

	Transactions(Database db) {
		this.db = db;
	}

	long clock() {
		return clock.incrementAndGet();
	}

	int nextNum(boolean readonly) {
		int n;
		do // loop needed for concurrency
			n = nextNum.incrementAndGet();
			while ((n % 2) != (readonly ? 0 : 1));
			// client expects readonly tran# to be even, update to be odd
		return n;
	}

	// used by tests
	void checkTransEmpty() {
		assert trans.isEmpty();
		assert rwtrans.isEmpty();
		assert finals.isEmpty();
		locks.checkEmpty();
	}

	synchronized void add(Transaction t) {
		trans.add(t);
		if (t.isReadWrite())
			rwtrans.add(t);
	}

	synchronized void addFinal(Transaction t) {
		assert t.isReadWrite();
		finals.add(t);
	}

	/**
	 * Remove transaction from outstanding.
	 * Called by {@link SuTransaction.complete} and {@link SuTransaction.abort}.
	 */
	synchronized void remove(Transaction t) {
		verify(trans.remove(t));
		if (t.isReadWrite())
			verify(rwtrans.remove(t));
		if (t.isCommitted())
			locks.removeWriteLocks(t);
		else // abort
			locks.remove(t);
		finalization();
	}

	/**
	 * Finalize any update transactions that are obsolete
	 * i.e. older than the oldest outstanding update transaction.
	 */
	private void finalization() {
		long oldest = rwtrans.isEmpty() ? FUTURE : rwtrans.peek().asof();
		while (! finals.isEmpty() && finals.peek().commitTime() < oldest)
			locks.remove(finals.poll());
		assert ! rwtrans.isEmpty() || finals.isEmpty();
		assert ! rwtrans.isEmpty() || locks.isEmpty();
	}

	// should be called periodically
	void limitOutstanding() {
		limitFinalsSize();
		abortStaleTrans();
	}

	/** if finals get too large abort oldest update transaction */
	private void limitFinalsSize() {
		Transaction t = null;
		synchronized(this) {
			if (finals.size() <= MAX_FINALS_SIZE)
				return;
			t = rwtrans.peek();
		}
		t.abortIfNotComplete("too many concurrent update transactions");
		Print.timestamped("aborted " + t + " - finals too large");
	}

	/** abort inactive transactions that accumulate too many shadows */
	private void abortStaleTrans() {
		Transaction t = null;
		synchronized (this) {
			if (trans.isEmpty())
				return;
			for (Transaction tran : trans) {
				int newShadowsSinceActivity = tran.shadowsSize() - tran.shadowSizeAtLastActivity();
				if (newShadowsSinceActivity > MAX_SHADOWS_SINCE_ACTIVITY) {
					t = tran;
					break;
				}
			}
		}
		// abort outside synchronized to avoid deadlock
		if (t != null) {
			t.abortIfNotComplete("inactive too long");
			Print.timestamped("aborted " + t + " - inactive too long");
		}
	}

	/**
	 * Called only by {@link SuTransaction.writeBtreeNodes}
	 * Gives other outstanding transactions shadow copies of any btree nodes
	 * that this transaction is going to update so they don't see the updates
	 * i.e. to implement snapshot isolation
	 */
	synchronized void addShadows(Transaction tcompleting, Long offset) {
		ByteBuf copy = null;
		for (Transaction t : trans)
			if (t != tcompleting)
				copy = t.shadow(db.dest, offset, copy);
	}

	synchronized Transaction readLock(Transaction tran, long offset) {
		return locks.addRead(tran, offset);
	}

	/** @return set of readlocks for item */
	synchronized Set<Transaction> writeLock(Transaction tran, long offset) {
		return locks.addWrite(tran, offset);
	}

	synchronized Set<Transaction> writes(long offset) {
		return locks.writes(offset);
	}

	synchronized List<Integer> tranlist() {
		List<Integer> list = new ArrayList<Integer>(trans.size());
		for (Transaction t : trans)
			list.add(t.num);
		return list;
	}

	int finalSize() {
		return finals.size();
	}

}
