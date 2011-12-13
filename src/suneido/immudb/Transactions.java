/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Verify.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import suneido.language.builtin.SuTransaction;
import suneido.util.Print;

import com.google.common.collect.Sets;

/**
 * Manages transactions. Uses {@link Locks}
 * Only deals with {@link UpdateTransactions}
 */
// NOTE: don't call any synchronized UpdateTransaction methods while synchronized
// because this can lead to deadlock.
@ThreadSafe
class Transactions {
	private final AtomicLong clock = new AtomicLong();
	private final AtomicInteger nextNum = new AtomicInteger();
	private final Locks locks = new Locks();
	/** all active transactions */
	private final Set<UpdateTransaction> trans = Sets.newHashSet();
	/** active read-write transactions */
	private final PriorityQueue<UpdateTransaction> rwtrans =
			new PriorityQueue<UpdateTransaction>(MAX_FINALS_SIZE, UpdateTransaction.byAsof);
	/** committed read-write transactions waiting to be finalized */
	private final PriorityQueue<UpdateTransaction> finals =
			new PriorityQueue<UpdateTransaction>(MAX_FINALS_SIZE, UpdateTransaction.byCommit);
	private static final long FUTURE = Long.MAX_VALUE;
	// only overridden by tests, otherwise could be private final
	static int MAX_FINALS_SIZE = 200;

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
	synchronized void checkTransEmpty() {
		assert trans.isEmpty();
		assert rwtrans.isEmpty();
		assert finals.isEmpty();
		locks.checkEmpty();
	}

	synchronized void add(UpdateTransaction tran) {
		trans.add(tran);
		rwtrans.add(tran);
	}

	synchronized void addFinal(UpdateTransaction tran) {
		finals.add(tran);
	}

	/**
	 * Remove transaction from outstanding.
	 * Called by {@link SuTransaction.complete} and {@link SuTransaction.abort}.
	 */
	synchronized void remove(UpdateTransaction tran) {
		verify(trans.remove(tran));
		verify(rwtrans.remove(tran));
		if (tran.isCommitted())
			locks.commit(tran);
		else
			locks.remove(tran);
		finalization();
	}

	/**
	 * Finalize any completed transactions that are obsolete
	 * i.e. older than the oldest outstanding update transaction.
	 */
	private void finalization() {
		long oldest = rwtrans.isEmpty() ? FUTURE : rwtrans.peek().asof();
		while (! finals.isEmpty() && finals.peek().commitTime() <= oldest)
			locks.remove(finals.poll());
		assert ! rwtrans.isEmpty() || finals.isEmpty();
		assert ! rwtrans.isEmpty() || locks.isEmpty();
	}

	// should be called periodically
	void limitOutstanding() {
		UpdateTransaction t = null;
		synchronized (this) {
			if (finals.size() <= MAX_FINALS_SIZE)
				return;
			t = rwtrans.peek();
		}
		// abort outside synchronized to avoid deadlock
		t.abortIfNotComplete("too many concurrent update transactions");
		Print.timestamped("aborted " + t + " - finals too large");
	}

	synchronized UpdateTransaction readLock(UpdateTransaction tran, int adr) {
		return locks.addRead(tran, adr);
	}

	synchronized Set<UpdateTransaction> writeLock(UpdateTransaction tran, int adr) {
		return locks.addWrite(tran, adr);
	}

	synchronized Set<UpdateTransaction> writes(int adr) {
		return locks.writes(adr);
	}

	synchronized List<Integer> tranlist() {
		List<Integer> list = new ArrayList<Integer>(trans.size());
		for (UpdateTransaction t : trans)
			list.add(t.num());
		return list;
	}

	synchronized int finalSize() {
		return finals.size();
	}

	/** for tests */
	boolean isLocksEmpty() {
		return locks.isEmpty();
	}

	@Override
	public String toString() {
		return locks.toString();
	}

}
