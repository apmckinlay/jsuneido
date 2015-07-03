/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Verify.verify;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.intfc.database.Transaction;
import suneido.util.Errlog;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Manages transactions.
 * Mostly for {@link UpdateTransactions}
 */
@ThreadSafe
class Transactions {
	private final AtomicLong clock = new AtomicLong(1); // zero is reserved
	private final AtomicInteger nextNum = new AtomicInteger();
	/** all active transactions (read and update), for Database.Transactions() */
	private final Set<Transaction> trans = Sets.newHashSet();
	/** active update transactions by asof (when they started)
	 *  used to track oldest active update transaction */
	private final TreeSet<UpdateTransaction> utrans =
			new TreeSet<>(UpdateTransaction.byAsof);
	/** committed update transactions that overlap active transactions */
	private final TreeSet<UpdateTransaction> overlapping =
			new TreeSet<>(UpdateTransaction.byCommit);
	private static final long FUTURE = Long.MAX_VALUE;
	private static final int MAX_OVERLAPPING = 200;
	static int MAX_UPDATE_TRAN_DURATION_SEC = 10;
	private volatile boolean exclusive = false;
	private final Deque<UpdateTransaction> commitLog = new ArrayDeque<>();

	long clock() {
		return clock.incrementAndGet();
	}

	int nextNum(boolean readonly) {
		int n;
		do // loop needed for concurrency
			n = nextNum.incrementAndGet();
			while ((n % 2) != (readonly ? 0 : 1));
			// client requires readonly tran# to be even, update to be odd
		return n;
	}

	// used by tests
	synchronized void checkTransEmpty() {
		assert trans.isEmpty() : "trans " + trans;
		assert utrans.isEmpty() : "utrans " + utrans;
		assert overlapping.isEmpty() : "overlapping " + overlapping;
	}

	synchronized void add(Transaction t) {
		trans.add(t);
	}

	synchronized void addUpdateTran(UpdateTransaction t) {
		if (exclusive)
			throw new SuException("blocked by exclusive transaction");
		assert t.asof() > 0;
		utrans.add(t);
	}

	synchronized void setExclusive(Transaction t) {
		if ((t instanceof BulkTransaction)
				? ! utrans.isEmpty()
				: utrans.size() != 1 || utrans.first() != t)
			throw new SuException("cannot make transaction exclusive");
		exclusive = true;
	}

	/** return the set of transactions that committed since asof */
	synchronized Set<UpdateTransaction> getOverlapping(long asof) {
		Set<UpdateTransaction> result = getOverlapping2(asof);
		HashSet<UpdateTransaction> fromLog = new HashSet<>();
		for (UpdateTransaction t : commitLog)
			if (t.commitTime() > asof)
				fromLog.add(t);
			else
				break;
		assert result.equals(fromLog);
		return result;
	}

	Set<UpdateTransaction> getOverlapping2(long asof) {
		if (overlapping.isEmpty())
			return Collections.emptySet();
		boolean inclusive = true;
		UpdateTransaction t = overlapping.first();
		Iterator<UpdateTransaction> iter = overlapping.descendingIterator();
		while (iter.hasNext()) {
			t = iter.next();
			if (t.commitTime() < asof) {
				inclusive = false;
				break;
			}
		}
		return ImmutableSet.copyOf(overlapping.tailSet(t, inclusive));
	}

	synchronized void commit(Transaction t) {
		if (t instanceof ReadWriteTransaction)
			exclusive = false;
		verify(trans.remove(t));
		if (t instanceof UpdateTransaction) {
			verify(utrans.remove(t));
			cleanOverlapping();
			if (! utrans.isEmpty())
				overlapping.add((UpdateTransaction) t);
			commitLog.addFirst((UpdateTransaction) t);
			while (commitLog.getLast().stopwatch.elapsed(TimeUnit.SECONDS)
					> 2 * MAX_UPDATE_TRAN_DURATION_SEC)
				commitLog.removeLast();
		}
	}

	synchronized void abort(Transaction t) {
		if (t instanceof ReadWriteTransaction)
			exclusive = false;
		verify(trans.remove(t));
		if (t instanceof UpdateTransaction)
			verify(utrans.remove(t));
		cleanOverlapping();
	}

	/**
	 * Remove transactions from overlapping that no longer overlap
	 * i.e. commitTime before the oldest outstanding update transaction.
	 */
	private void cleanOverlapping() {
		long oldest = utrans.isEmpty() ? FUTURE : utrans.first().asof();
		while (! overlapping.isEmpty() && overlapping.first().commitTime() <= oldest)
			overlapping.remove(overlapping.first());
		assert ! utrans.isEmpty() || overlapping.isEmpty();
	}

	// should be called periodically
	void limitOutstanding() {
		limitOverlapping();
		limitUpdateDuration();
	}

	private void limitOverlapping() {
		UpdateTransaction t = null;
		synchronized (this) {
			if (overlapping.size() <= MAX_OVERLAPPING)
				return;
			t = utrans.first();
		}
		// abort outside synchronized to avoid deadlock
		abort(t, "too many concurrent update transactions");
	}

	private void limitUpdateDuration() {
		if (exclusive)
			return;
		while (true) {
			UpdateTransaction t = null;
			synchronized (this) {
				if (utrans.isEmpty())
					return;
				t = utrans.first();
				if (t.stopwatch.elapsed(TimeUnit.SECONDS) < MAX_UPDATE_TRAN_DURATION_SEC)
					return;
			}
			// abort outside synchronized to avoid deadlock
			abort(t, "update transaction longer than " + MAX_UPDATE_TRAN_DURATION_SEC + " seconds");
		}
	}

	private static void abort(UpdateTransaction t, String msg) {
		t.abortIfNotComplete(msg);
		Errlog.errlog("aborted " + t + " - " + msg);
	}

	synchronized List<Integer> tranlist() {
		List<Integer> list = new ArrayList<>(trans.size());
		for (Transaction t : trans)
			list.add(t.num());
		return list;
	}

	synchronized int finalSize() {
		return overlapping.size();
	}

}
