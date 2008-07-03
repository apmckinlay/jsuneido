package suneido.database;

import static suneido.Suneido.verify;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

public class Transactions {
	public final Database db;
	private long clock = 0;
	private int nextNum = 0;
	private final HashMap<Integer, Transaction> trans = new HashMap<Integer, Transaction>();
	private final PriorityQueue<Transaction> trans2 = new PriorityQueue<Transaction>();
	private final HashMap<Long,Long> created = new HashMap<Long,Long>();	// record address -> create time
	private final HashMap<Long,TranDelete> deleted = new HashMap<Long,TranDelete>();	// record address -> delete time
	private final ArrayDeque<Transaction> finals = new ArrayDeque<Transaction>();

	public static final long FUTURE = Long.MAX_VALUE;
	public static final long UNCOMMITTED = Long.MAX_VALUE / 2;
	public static final long PAST = Long.MIN_VALUE;

	Transactions(Database db) {
		this.db = db;
	}

	public Transaction readonlyTran() {
		return newTran(true);
	}

	public Transaction readwriteTran() {
		return newTran(false);
	}

	private Transaction newTran(boolean readonly) {
		int num = ++nextNum;
		Transaction tran = new Transaction(this, readonly, clock(), num);
		trans.put(num, tran);
		trans2.add(tran);
		verify(trans.size() == trans2.size());
		return tran;
	}

	public long clock() {
		return ++clock;
	}

	/**
	 * Finalize any update transactions that are older than the oldest
	 * outstanding transaction.
	 */
	private void finalization() {
		long oldest = trans.isEmpty() ? FUTURE : trans2.peek().asof();
		assert trans.isEmpty()
				|| oldest == Collections.min(trans.values()).asof();
		while (!finals.isEmpty() && finals.peekFirst().asof() < oldest)
			finals.removeFirst().finalization();
	}

	public void putCreated(long adr, long t) {
		created.put(adr, UNCOMMITTED + t);
	}

	long create_time(long off) {
		Long t = created.get(off);
		return t == null ? PAST : t;
	}

	public void updateCreated(long off, long commit_time) {
		Long t = created.get(off);
		verify(t != null && t > UNCOMMITTED);
		created.put(off, commit_time);
	}

	public void removeCreated(long off) {
		verify(created.remove(off) != null);
	}

	public void putDeleted(long adr, long t) {
		deleted.put(adr, new TranDelete(t));
	}

	public void updateDeleted(long off, long commit_time) {
		TranDelete td = deleted.get(off);
		verify(td != null && td.time > UNCOMMITTED);
		td.time = commit_time;
	}

	public void removeDeleted(long off) {
		verify(deleted.remove(off) != null);
	}

	long delete_time(long off) {
		TranDelete td = deleted.get(off);
		return td == null ? FUTURE : td.time;
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

	public void ended(Transaction tran) {
		verify(trans.remove(tran.num) != null);
		verify(trans2.remove(tran));
		verify(trans.size() == trans2.size());
		finalization();
	}

	public void addFinal(Transaction tran) {
		// finals should be sorted
		verify(finals.isEmpty() || tran.asof() > finals.peekLast().asof());
		finals.add(tran);
	}

	/**
	 * abort all outstanding transactions
	 */
	public void shutdown() {
		for (Transaction tran : trans.values())
			tran.abort();
	}

	public String anyConflicts(long asof, int tblnum, short[] colnums,
			Record from,
			Record to) {
		Iterator<Transaction> iter = finals.descendingIterator();
		while (iter.hasNext()) {
			Transaction t = iter.next();
			if (t.asof() <= asof)
				break;
			for (TranWrite tw : t.writes) {
				if (tw.tblnum != tblnum)
					continue ;
				Record rec = db.input(tw.off);
				Record key = rec.project(colnums);
				if (key.inRange(from, to))
					return read_conflict(t, tblnum, from, to, key);
			}
		}
		return "";
	}

	private String read_conflict(Transaction t, int tblnum, Record from,
			Record to, Record key) {
		// TODO Auto-generated method stub
		return "read conflict";
	}
}
