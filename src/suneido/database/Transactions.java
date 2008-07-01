package suneido.database;

import static suneido.Suneido.verify;

import java.util.HashMap;

public class Transactions {
	private long clock = 0;
	private HashMap<Long,Long> created = new HashMap<Long,Long>();	// record address -> create time
	private HashMap<Long,TranDelete> deleted = new HashMap<Long,TranDelete>();	// record address -> delete time

	public static final long FUTURE = Long.MAX_VALUE;
	public static final long UNCOMMITTED = Long.MAX_VALUE / 2;
	public static final long PAST = Long.MIN_VALUE;

	public Transaction readonlyTran() {
		return new Transaction(this, true, ++clock);
	}

	public Transaction readwriteTran() {
		return new Transaction(this, false, ++clock);
	}
	
	long create_time(long off) {
		Long t = created.get(off);
		return t == null ? PAST : t;
	}
	
	long delete_time(long off) {
		TranDelete td = deleted.get(off);
		return td == null ? FUTURE : td.time;
	}

	private boolean cleanup() {
		return true; // TODO
	}
	

	public void create_act(int tblnum, long adr, long t) {
		created.put(adr, UNCOMMITTED + t);
//		t->acts.push_back(TranAct(CREATE_ACT, tblnum, adr, clock));
	}

	public boolean delete_act(int tblnum, long adr, long t) {
		TranDelete td = deleted.get(adr);
		if (td != null) {
			verify(cleanup());
			return false;
			}
		deleted.put(adr, new TranDelete(t));
//		t->acts.push_back(TranAct(DELETE_ACT, tblnum, off, clock));
		return true;
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
