package suneido.database;

import java.util.HashMap;

public class Transactions {
	private long clock = 0;
	private final HashMap<Long,Long> created = new HashMap<Long,Long>();	// record address -> create time
	private final HashMap<Long,TranDelete> deleted = new HashMap<Long,TranDelete>();	// record address -> delete time

	public static final long FUTURE = Long.MAX_VALUE;
	public static final long UNCOMMITTED = Long.MAX_VALUE / 2;
	public static final long PAST = Long.MIN_VALUE;

	public Transaction readonlyTran() {
		return new Transaction(this, true, ++clock);
	}

	public Transaction readwriteTran() {
		return new Transaction(this, false, ++clock);
	}

	public long clock() {
		return clock;
	}

	long create_time(long off) {
		Long t = created.get(off);
		return t == null ? PAST : t;
	}

	long delete_time(long off) {
		TranDelete td = deleted.get(off);
		return td == null ? FUTURE : td.time;
	}

	/**
	 * Finalize any update transactions that are older than the oldest
	 * outstanding read transaction.
	 * 
	 * @return false if exception, otherwise true
	 */
	public boolean cleanup() {
		return true; // TODO
	}

	public void putCreated(long adr, long t) {
		created.put(adr, UNCOMMITTED + t);
	}

	public void putDeleted(long adr, long t) {
		deleted.put(adr, new TranDelete(t));
	}

	public String deleteConflict(long adr) {
		TranDelete td = deleted.get(adr);
		return td == null ? "" : write_conflict(td);
	}

	private String write_conflict(TranDelete td) {
		// TODO Auto-generated method stub
		return "delete conflict";
	}

	private static class TranDelete {
		long t;
		long time;
		TranDelete(long t) {
			this.t = t;
			this.time = t + UNCOMMITTED;
		}
	}
}
