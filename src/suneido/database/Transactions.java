package suneido.database;

import static suneido.Suneido.verify;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.jcip.annotations.ThreadSafe;
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
	private final HashMap<Integer, Transaction> trans = new HashMap<Integer, Transaction>();

	// used to determine the oldest outstanding transaction
	private final PriorityQueue<Transaction> trans2 = new PriorityQueue<Transaction>();

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
		assert trans2.isEmpty();
		assert finals.isEmpty();
		locks.checkEmpty();
	}

	synchronized public void add(Transaction tran) {
		trans.put(tran.num, tran);
		trans2.add(tran);
		verify(trans.size() == trans2.size());
	}

	synchronized public void addFinal(Transaction tran) {
		finals.add(tran);
	}

	/**
	 * Remove transaction from outstanding.
	 * Called by {@link Transaction.complete} and {@link Transaction.abort}.
	 */
	synchronized public void remove(Transaction tran) {
		verify(trans.remove(tran.num) != null);
		verify(trans2.remove(tran));
		verify(trans.size() == trans2.size());
		if (tran.isCommitted())
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
		long oldest = trans.isEmpty() ? FUTURE : trans2.peek().asof();
		while (!finals.isEmpty() && finals.peek().asof() < oldest)
			locks.remove(finals.poll());
		assert !trans.isEmpty() || finals.isEmpty();
		assert !trans.isEmpty() || locks.isEmpty();
	}

	synchronized public void shutdown() {
		// abort all outstanding transactions
		while (!trans2.isEmpty())
			trans2.peek().abort();
	}

	/** Called only by {@Transaction.completeReadWrite}
	 * Gives other outstanding transactions shadow copies of any btree nodes
	 * that this transaction is going to update so they don't see the updates
	 * i.e. to implement snapshot isolation
	 */
	synchronized void addShadows(Transaction tcompleting, Map<Long, ByteBuf> shadow) {
		for (Map.Entry<Long, ByteBuf> e : shadow.entrySet()) {
			Long offset = e.getKey();
			ByteBuf copy = null;
			for (Transaction t : trans2) {
				if (t == tcompleting)
					continue;
				if (t.shadow.containsKey(offset))
					continue;
				if (copy == null)
					copy = db.dest.adr(offset).copy(Btree.NODESIZE)
							.asReadOnlyBuffer(); // shared read-only copy
				t.shadow.put(offset, copy);
			}
		}
	}

	synchronized public Transaction readLock(Transaction tran, long offset) {
		return locks.addRead(tran, offset);
	}

	synchronized public Set<Transaction> writeLock(Transaction tran, long offset) {
		return locks.addWrite(tran, offset);
	}

	synchronized public Set<Transaction> writes(long offset) {
		return locks.writes(offset);
	}

}
