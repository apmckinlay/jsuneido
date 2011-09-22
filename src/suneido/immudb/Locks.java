/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

/** Synchronized by {@link Transactions} which is the only user */
@NotThreadSafe
class Locks {
	private final SetMultimap<UpdateTransaction, Integer> locksRead = HashMultimap.create();
	private final SetMultimap<UpdateTransaction, Integer> locksWrite = HashMultimap.create();
	private final SetMultimap<Integer, UpdateTransaction> readLocks = HashMultimap.create();
	private final SetMultimap<Integer, UpdateTransaction> writes = HashMultimap.create();
	private final TIntObjectHashMap<UpdateTransaction> writeLocks =
			new TIntObjectHashMap<UpdateTransaction>();

	/** Add a read lock, unless there's already a write lock.
	 *  @return A transaction if it has a write lock on this adr, else null
	 */
	UpdateTransaction addRead(UpdateTransaction tran, int adr) {
		if (tran.isEnded())
			return null;
		UpdateTransaction prev = writeLocks.get(adr);
		if (prev == tran)
			return null; // don't need readLock if already have writeLock
		readLocks.put(adr, tran);
		locksRead.put(tran, adr);
		return prev;
	}

	/** @return null if another transaction already has write lock,
	 *  or else a set (possibly empty) of other transactions that have read locks.
	 *  NOTE: The set may contain the locking transaction.
	 */
	ImmutableSet<UpdateTransaction> addWrite(UpdateTransaction tran, int adr) {
		if (tran.isEnded())
			return null;
		UpdateTransaction prev = writeLocks.get(adr);
		if (prev == tran)
			return ImmutableSet.of(); // already have write lock
		if (prev != null)
			return null; // write-write conflict
		for (UpdateTransaction w : writes.get(adr)) {
			if (w != tran && ! w.committedBefore(tran))
				return null; // write-write conflict with completed
		}
		writeLocks.put(adr, tran);
		writes.put(adr, tran);
		readLocks.remove(adr, tran); // read lock not needed when write lock
		locksWrite.put(tran, adr);
		return ImmutableSet.copyOf(readLocks.get(adr));
	}

	/** Just remove writeLocks. Other locks kept till finalization */
	void commit(UpdateTransaction tran) {
		for (Integer adr : locksWrite.get(tran))
			writeLocks.remove(adr);
	}

	/** Remove all locks for this transaction */
	void remove(UpdateTransaction tran) {
		for (Integer adr : locksRead.get(tran))
			readLocks.remove(adr, tran);
		locksRead.removeAll(tran);
		assert !locksRead.isEmpty() || readLocks.isEmpty();

		for (Integer adr : locksWrite.get(tran)) {
			writeLocks.remove(adr);
			writes.remove(adr, tran);
		}
		locksWrite.removeAll(tran);
		assert !locksWrite.isEmpty() || (writeLocks.isEmpty() && writes.isEmpty());
	}

	Set<UpdateTransaction> writes(int adr) {
		return ImmutableSet.copyOf(writes.get(adr));
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

	boolean contains(UpdateTransaction tran) {
		return locksRead.containsKey(tran) || locksWrite.containsKey(tran);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Locks").append("{");
		TIntObjectIterator<UpdateTransaction> iter = writeLocks.iterator();
		while (iter.hasNext()) {
			iter.advance();
			sb.append(" " + iter.value() + "-w" + iter.key());
		}
		for (Map.Entry<Integer, UpdateTransaction> e : readLocks.entries())
			sb.append(" " + e.getValue() + "-r" + e.getKey());
		return sb.append(" }").toString();
	}

}
