package suneido.database;

import static suneido.SuException.verify;

import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import suneido.util.ByteBuf;

/**
 * Manages transactions.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
@ThreadSafe
public class Transactions {
	public final Database db;
	private final AtomicLong clock = new AtomicLong();
	private final AtomicInteger nextNum = new AtomicInteger();
	private final Locks locks = new Locks();
	private final PriorityQueue<Transaction> trans = new PriorityQueue<Transaction>();
	private final PriorityQueue<Transaction> finals = new PriorityQueue<Transaction>();
	static final long FUTURE = Long.MAX_VALUE;

	Transactions(Database db) {
		this.db = db;
	}

	public long clock() {
		return clock.incrementAndGet();
	}

	public int nextNum() {
		return nextNum.incrementAndGet();
	}

	// used by tests
	public void checkTransEmpty() {
		assert trans.isEmpty();
		assert finals.isEmpty();
		locks.checkEmpty();
	}

	synchronized public void add(Transaction tran) {
		trans.add(tran);
	}

	synchronized public void addFinal(Transaction tran) {
		finals.add(tran);
	}

	/**
	 * Remove transaction from outstanding.
	 * Called by {@link Transaction.complete} and {@link Transaction.abort}.
	 */
	synchronized public void remove(Transaction tran) {
		verify(trans.remove(tran));
		if (tran.isReadWrite() && tran.isCommitted())
			locks.commit(tran);
		else
			locks.remove(tran);
		finalization();
	}

	/**
	 * Finalize any update transactions that are obsolete
	 * i.e. older than the oldest outstanding transaction.
	 */
	private void finalization() {
		long oldest = trans.isEmpty() ? FUTURE : trans.peek().asof();
		while (!finals.isEmpty() && finals.peek().asof() < oldest)
			locks.remove(finals.poll());
		assert !trans.isEmpty() || finals.isEmpty();
		assert !trans.isEmpty() || locks.isEmpty();
	}

	synchronized public void shutdown() {
		// abort all outstanding transactions
		while (!trans.isEmpty())
			trans.peek().abort();
	}

	/**
	 * Called only by {@link Transaction.writeBtreeNodes}
	 * Gives other outstanding transactions shadow copies of any btree nodes
	 * that this transaction is going to update so they don't see the updates
	 * i.e. to implement snapshot isolation
	 */
	synchronized void addShadows(Transaction tcompleting, Long offset) {
		ByteBuf copy = null;
		for (Transaction t : trans)
			if (t != tcompleting)
				copy = t.shadows.shadow(db.dest, offset, copy);
	}

	public synchronized Transaction readLock(Transaction tran, long offset) {
		return locks.addRead(tran, offset);
	}

	public synchronized Set<Transaction> writeLock(Transaction tran, long offset) {
		return locks.addWrite(tran, offset);
	}

	public synchronized Set<Transaction> writes(long offset) {
		return locks.writes(offset);
	}

}
