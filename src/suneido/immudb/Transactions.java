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

/**
 * Manages transactions. Uses {@link Locks}
 */
// NOTE: don't call any synchronized UpdateTransaction methods while synchronized
// because this can lead to deadlock.
@ThreadSafe
class Transactions {
	private final AtomicLong clock = new AtomicLong();
	private final AtomicInteger nextNum = new AtomicInteger();
	private final Locks locks = new Locks();
	private final PriorityQueue<UpdateTransaction> trans = new PriorityQueue<UpdateTransaction>();
	private final PriorityQueue<UpdateTransaction> finals = new PriorityQueue<UpdateTransaction>();
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
		assert finals.isEmpty();
		locks.checkEmpty();
	}

	synchronized void add(UpdateTransaction tran) {
		trans.add(tran);
	}

	synchronized void addFinal(UpdateTransaction tran) {
		assert tran.isReadWrite();
		finals.add(tran);
	}

	/**
	 * Remove transaction from outstanding.
	 * Called by {@link SuTransaction.complete} and {@link SuTransaction.abort}.
	 */
	synchronized void remove(UpdateTransaction tran) {
		verify(trans.remove(tran));
		if (tran.isReadWrite() && tran.isCommitted())
			locks.commit(tran);
		else
			locks.remove(tran);
		finalization();
	}

	/**
	 * Finalize any completed update transactions that are obsolete
	 * i.e. older than the oldest outstanding update transaction.
	 */
	private void finalization() {
		long oldest = oldestReadWriteTran();
		while (!finals.isEmpty() && finals.peek().asof() < oldest)
			locks.remove(finals.poll());
		assert !trans.isEmpty() || finals.isEmpty();
		assert !trans.isEmpty() || locks.isEmpty();
	}

	private long oldestReadWriteTran() {
		for (UpdateTransaction t : trans)
			if (t.isReadWrite())
				return t.asof();
		return FUTURE;
	}

	// should be called periodically
	void limitOutstanding() {
		UpdateTransaction t = null;
		synchronized (this) {
			if (finals.size() <= MAX_FINALS_SIZE)
				return;
			for (UpdateTransaction tran : trans)
				if (tran.isReadWrite()) {
					t = tran;
					break;
				}
		}
		// abort outside synchronized to avoid deadlock
		if (t != null) {
			t.abortIfNotComplete("too many concurrent update transactions");
			Print.timestamped("aborted " + t + " - finals too large");
		}
	}

	synchronized UpdateTransaction readLock(UpdateTransaction tran, long offset) {
		return locks.addRead(tran, offset);
	}

	synchronized Set<UpdateTransaction> writeLock(UpdateTransaction tran, long offset) {
		return locks.addWrite(tran, offset);
	}

	synchronized Set<UpdateTransaction> writes(long offset) {
		return locks.writes(offset);
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

}
