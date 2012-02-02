/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Verify.verify;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import suneido.intfc.database.Transaction;
import suneido.util.Print;

import com.google.common.collect.Sets;

/**
 * Manages transactions.
 * Mostly deals with {@link UpdateTransactions}
 */
@ThreadSafe
class Transactions2 {
	private final AtomicLong clock = new AtomicLong();
	private final AtomicInteger nextNum = new AtomicInteger();
	/** all active transactions (read and update), for Database.Transactions() */
	private final Set<Transaction> trans = Sets.newHashSet();
	/** active update transactions */
	private final PriorityQueue<UpdateTransaction> utrans =
			new PriorityQueue<UpdateTransaction>(MAX_OVERLAPPING, UpdateTransaction.byAsof);
	/** committed read-write transactions that overlap outstanding transactions */
	private final TreeSet<UpdateTransaction> overlapping =
			new TreeSet<UpdateTransaction>(UpdateTransaction.byCommit);
	private static final long FUTURE = Long.MAX_VALUE;
	// only overridden by tests, otherwise could be private final
	static int MAX_OVERLAPPING = 200;

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
		assert utrans.isEmpty();
		assert overlapping.isEmpty();
	}

	synchronized void add(Transaction t) {
		trans.add(t);
		if (t instanceof UpdateTransaction)
			utrans.add((UpdateTransaction) t);
	}

	synchronized void commit(Transaction t) {
		verify(trans.remove(t));
		if (t instanceof UpdateTransaction) {
			verify(utrans.remove(t));
			if (! utrans.isEmpty())
				overlapping.add((UpdateTransaction) t);
		}
	}

	synchronized void abort(Transaction t) {
		verify(trans.remove(t));
		verify(utrans.remove(t));
		cleanOverlapping();
	}

	/**
	 * Remove transactions from overlapping that no longer overlap
	 * i.e. commitTime before the oldest outstanding update transaction.
	 */
	private void cleanOverlapping() {
		long oldest = utrans.isEmpty() ? FUTURE : utrans.peek().asof();
		while (! overlapping.isEmpty() && overlapping.first().commitTime() <= oldest)
			overlapping.remove(overlapping.first());
		assert ! utrans.isEmpty() || overlapping.isEmpty();
	}

	// should be called periodically
	void limitOutstanding() {
		UpdateTransaction t = null;
		synchronized (this) {
			if (overlapping.size() <= MAX_OVERLAPPING)
				return;
			t = utrans.peek();
		}
		// abort outside synchronized to avoid deadlock
		t.abortIfNotComplete("too many concurrent update transactions");
		Print.timestamped("aborted " + t + " - finals too large");
	}

	synchronized List<Integer> tranlist() {
		List<Integer> list = new ArrayList<Integer>(trans.size());
		for (Transaction t : trans)
			list.add(t.num());
		return list;
	}

	synchronized int finalSize() {
		return overlapping.size();
	}

//	@Override
//	public String toString() {
//		return locks.toString();
//	}

}
