package suneido.database;

import static suneido.Suneido.verify;

import java.util.*;

import com.google.common.collect.ImmutableList;

/**
 * Manages transactions.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
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

	public long clock() {
		return ++clock;
	}

	public int nextNum() {
		return ++nextNum;
	}

	public void add(Transaction tran) {
		trans.put(tran.num, tran);
		trans2.add(tran);
		verify(trans.size() == trans2.size());
	}

	public void addFinal(Transaction tran) {
		// finals should be sorted
		verify(finals.isEmpty() || tran.asof() > finals.peekLast().asof());
		finals.add(tran);
	}

	/**
	 * Remove Transaction from outstanding.
	 * Called by {@link Transaction.complete} and {@link Transaction.abort}.
	 * @param tran
	 */
	public void remove(Transaction tran) {
		verify(trans.remove(tran.num) != null);
		verify(trans2.remove(tran));
		verify(trans.size() == trans2.size());
		finalization();
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

	// track creation time of records ===============================
	public void putCreated(long adr, long t) {
		created.put(adr, UNCOMMITTED + t);
	}
	public long createTime(long off) {
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

	// track deletion time of records ===============================
	public void putDeleted(Transaction tran, long adr, long t) {
		deleted.put(adr, new TranDelete(tran, t));
	}
	public void updateDeleted(long off, long commit_time) {
		TranDelete td = deleted.get(off);
		assert (td != null && td.time > UNCOMMITTED);
		td.time = commit_time;
	}
	public void removeDeleted(Transaction tran, long off) {
		verify(deleted.remove(off).tran == tran);
	}
	public long deleteTime(long off) {
		TranDelete td = deleted.get(off);
		return td == null ? FUTURE : td.time;
	}
	public String deleteConflict(int tblnum, long adr) {
		TranDelete td = deleted.get(adr);
		if (td == null)
			return "";
		StringBuilder sb = new StringBuilder("delete conflict with ");
		sb.append(td.tran.sessionId);
		sb.append(" transaction ").append(td.tran.num);
		sb.append(" table: ");
		Table tbl = db.getTable(tblnum);
		if (tbl != null)
			{
			sb.append(tbl.name);
			Index index = tbl.indexes.firstKey();
			sb.append(" index: ").append(index.columns).append(" key ")
					.append(db.input(adr).project(index.colnums));
			}
		else
			sb.append(tblnum);
		return sb.toString();
	}
	private static class TranDelete {
		Transaction tran;
		long time;
		TranDelete(Transaction tran, long t) {
			this.tran = tran;
			this.time = t + UNCOMMITTED;
		}
	}

	/**
	 * abort all outstanding transactions
	 */
	public void shutdown() {
		while (!trans2.isEmpty())
			trans2.peek().abort();
	}

	public String anyConflicts(long asof, int tblnum,
			ImmutableList<Integer> colnums, Record from, Record to, String index) {
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
					return readConflict(t, tblnum, index, from, to, key,
							tw.type.toString());
			}
		}
		return null;
	}

	private String readConflict(Transaction t, int tblnum, String index,
			Record from, Record to, Record key, String type) {
		StringBuilder sb = new StringBuilder("read conflict with ");
		sb.append(t.sessionId);
		sb.append(" transaction ").append(t.num);
		sb.append(" table: ");
		Table tbl = db.getTable(tblnum);
		if (tbl == null)
			sb.append(tblnum);
		else
			sb.append(tbl.name);
		sb.append(" index: ").append(index).append(" from ").append(from)
				.append(" to ").append(to).append(" key ").append(key)
				.append(" ").append(type);
		return sb.toString();
	}
}
