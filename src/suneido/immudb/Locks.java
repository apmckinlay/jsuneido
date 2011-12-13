/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/** Synchronized by {@link Transactions} which is the only user */
@NotThreadSafe
class Locks {
	private final SetMultimap<UpdateTransaction, Integer> locksRead = HashMultimap.create();
	private final SetMultimap<UpdateTransaction, Integer> locksWrite = HashMultimap.create();
	private final SetMultimap<Integer, UpdateTransaction> readLocks = HashMultimap.create();
	private final SetMultimap<Integer, UpdateTransaction> writes = HashMultimap.create();
	private final Map<Integer, UpdateTransaction> writeLocks = Maps.newHashMap();

	/** Add a read lock, unless there's already a write lock.
	 *  @return A transaction if it has a write lock on this adr, else null
	 */
	UpdateTransaction addRead(UpdateTransaction t, int adr) {
		if (t.isEnded())
			return null;
		UpdateTransaction prev = writeLocks.get(adr);
		if (prev == t)
			return null; // don't need readLock if already have writeLock
		readLocks.put(adr, t);
		locksRead.put(t, adr);
		return prev;
	}

	/** @return the set (possibly empty) of transactions that have read locks */
	ImmutableSet<UpdateTransaction> addWrite(UpdateTransaction t, int a) {
		assert ! t.isEnded();
		Integer adr = a;
		UpdateTransaction prev = writeLocks.get(adr);
		if (prev == t)
			return ImmutableSet.of(); // already have write lock
		if (prev != null)
			t.abortThrow("conflict (write-write) " + t + " with " + prev);
		for (UpdateTransaction w : writes.get(adr))
			if (w != t && ! w.committedBefore(t))
				t.abortThrow("conflict (write-write) " + t + " with " + w);
		writeLocks.put(adr, t);
		writes.put(adr, t);
		readLocks.remove(adr, t); // read lock not needed when write lock
		locksWrite.put(t, adr);
		return ImmutableSet.copyOf(readLocks.get(adr));
	}

	/** Just remove writeLocks. Other locks kept till finalization */
	void commit(UpdateTransaction t) {
		for (Integer adr : locksWrite.get(t))
			writeLocks.remove(adr);
	}

	/** Remove all locks for this transaction */
	void remove(UpdateTransaction t) {
		for (Integer adr : locksRead.get(t))
			readLocks.remove(adr, t);
		locksRead.removeAll(t);
		assert !locksRead.isEmpty() || readLocks.isEmpty();

		for (Integer adr : locksWrite.get(t)) {
			writeLocks.remove(adr);
			writes.remove(adr, t);
		}
		locksWrite.removeAll(t);
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

	boolean contains(UpdateTransaction t) {
		return locksRead.containsKey(t) || locksWrite.containsKey(t);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Locks").append("{");
		for (Map.Entry<Integer, UpdateTransaction> e : writeLocks.entrySet())
			sb.append(" " + e.getValue() + "-w" + e.getKey());
		for (Map.Entry<Integer, UpdateTransaction> e : readLocks.entries())
			sb.append(" " + e.getValue() + "-r" + e.getKey());
		return sb.append(" }").toString();
	}

}
