/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.Suneido;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/** Synchronized by {@link Transactions} which is the only user */
@NotThreadSafe
class Locks {
	// TODO put locksRead and lockWrite in Transaction as TLongSet
	private final SetMultimap<Transaction, Long> locksRead = HashMultimap.create();
	private final SetMultimap<Transaction, Long> locksWrite = HashMultimap.create();
	private final SetMultimap<Long, Transaction> readLocks = HashMultimap.create();
	private final SetMultimap<Long, Transaction> writes = HashMultimap.create();
	private final Map<Long, Transaction> writeLocks = Maps.newHashMap();

	/** Add a read lock, unless there's already a write lock.
	 *  @return A transaction if it has a write lock on this adr, else null
	 */
	Transaction addRead(Transaction t, long a) {
		assert t.isReadWrite();
		assert ! t.isEnded();
		Long adr = a;
		Transaction prev = writeLocks.get(adr);
		if (prev == t)
			return null; // already have writeLock
		if (! Suneido.cmdlineoptions.snapshotIsolation) {
			readLocks.put(adr, t);
			locksRead.put(t, adr);
		}
		return prev;
	}

	/** @return the set (possibly empty) of transactions that have read locks */
	ImmutableSet<Transaction> addWrite(Transaction t, long a) {
		assert t.isReadWrite();
		assert ! t.isEnded();
		Long adr = a;
		Transaction prev = writeLocks.get(adr);
		if (prev == t)
			return ImmutableSet.of(); // already have write lock
		else if (prev != null)
			t.abortThrow("conflict (write-write) " + t + " with " + prev);
		for (Transaction w : writes.get(adr))
			if (w != t && ! w.committedBefore(t))
				t.abortThrow("conflict (write-write) " + t + " with " + w);
		// write-write also detected by shadows in DestTrans.nodeForWrite
		writes.put(adr, t);
		writeLocks.put(adr, t);
		locksWrite.put(t, adr);
		readLocks.remove(adr, t); // read lock not needed when write lock
		return ImmutableSet.copyOf(readLocks.get(adr));
	}

	ImmutableSet<Transaction> writes(long adr) {
		return ImmutableSet.copyOf(writes.get(adr));
	}

	/**
	 * Remove all locks for this transaction.
	 * Called by abort and finalization.
	 */
	void remove(Transaction t) {
		removeReads(t);
		removeWrites(t);
	}

	private void removeReads(Transaction t) {
		for (Long adr : locksRead.get(t))
			readLocks.remove(adr, t);
		locksRead.removeAll(t);
		assert !locksRead.isEmpty() || readLocks.isEmpty();
	}

	private void removeWrites(Transaction t) {
		removeWriteLocks(t);
		for (Long adr : locksWrite.get(t))
			writes.remove(adr, t);
		locksWrite.removeAll(t);
		assert ! locksWrite.isEmpty() || (writeLocks.isEmpty() && writes.isEmpty());
	}

	// called by commit
	// keep writes to detect write skew conflicts with committed transactions
	void removeWriteLocks(Transaction t) {
		for (Long adr : locksWrite.get(t))
			writeLocks.remove(adr);
	}

	boolean isEmpty() {
		return locksRead.isEmpty() && locksWrite.isEmpty();
	}

	void checkEmpty() {
		assert readLocks.isEmpty();
		assert writeLocks.isEmpty();
		assert writes.isEmpty();
		assert locksRead.isEmpty();
		assert locksWrite.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Locks");
		for (Map.Entry<Long, Transaction> e : writeLocks.entrySet())
			sb.append(" " + e.getValue() + "w" + e.getKey());
		for (Map.Entry<Long, Transaction> e : readLocks.entries())
			sb.append(" " + e.getValue() + "r" + e.getKey());
		return sb.toString();
	}

}
