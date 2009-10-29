package suneido.database;

import java.util.*;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import com.google.common.collect.*;

@ThreadSafe
public class Locks {

	@GuardedBy("this")
	private final SetMultimap<Transaction, Long> locks = HashMultimap.create();

	@GuardedBy("this")
	private final SetMultimap<Long, Transaction> readLocks = HashMultimap.create();

	@GuardedBy("this")
	private final Map<Long, Transaction> writeLocks = new HashMap<Long, Transaction>();

	@GuardedBy("this")
	private final SetMultimap<Long, Transaction> writes = HashMultimap.create();

	/** @return A transaction if it has a write lock on this adr,
	 * otherwise null
	 */
	synchronized public Transaction addRead(Transaction tran, long adr) {
		Transaction prev = writeLocks.get(adr);
		if (prev == tran)
			return null;
		readLocks.put(adr, tran);
		locks.put(tran, adr);
		return prev;
	}

	/** @return null if another transaction already has write lock,
	 *  or else a set (possibly empty) of other transactions that have read locks.
	 *  NOTE: The set may contain the locking transaction.
	 */
	synchronized public ImmutableSet<Transaction> addWrite(Transaction tran, long adr) {
		Transaction prev = writeLocks.get(adr);
		if (prev != null && prev != tran)
			return null; // write-write conflict
		writeLocks.put(adr, tran);
		writes.put(adr, tran);
		readLocks.remove(adr, tran); // read lock not needed when write lock
		locks.put(tran, adr);
		return ImmutableSet.copyOf(readLocks.get(adr));
	}

	/** Just remove writeLocks. Other locks kept till finalization */
	synchronized public void commit(Transaction tran) {
		for (Long adr : locksFor(tran))
			writeLocks.remove(adr);
	}

	/** Remove all locks for this transaction */
	synchronized public void remove(Transaction tran) {
		for (Long adr : locksFor(tran)) {
			readLocks.remove(adr, tran);
			writeLocks.remove(adr);
			writes.remove(adr, tran);
		}
		locks.removeAll(tran);
		assert !locks.isEmpty()
				|| (readLocks.isEmpty() && writeLocks.isEmpty() && writes.isEmpty());
	}

	private Set<Long> locksFor(Transaction tran) {
		Set<Long> adrs = locks.get(tran);
		if (adrs == null)
			adrs = Collections.emptySet();
		return adrs;
	}

	public Set<Transaction> writes(long offset) {
		return ImmutableSet.copyOf(writes.get(offset));
	}

	synchronized public boolean isEmpty() {
		return locks.isEmpty();
	}

}
