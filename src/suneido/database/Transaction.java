package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Transactions.FUTURE;
import static suneido.database.Transactions.UNCOMMITTED;

import java.nio.ByteBuffer;
import java.util.*;

import suneido.SuException;
import suneido.database.server.DbmsTran;
import suneido.util.PersistentMap;

import com.google.common.collect.ImmutableList;

/**
 * Handles a single transaction, either readonly or readwrite.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Transaction implements Comparable<Transaction>, DbmsTran {
	private final Transactions trans;
	private final boolean readonly;
	protected boolean ended = false;
	private String conflict = null;
	private final long t;
	private long asof; // not final because updated when readwrite tran commits
	String sessionId = "session";
	private final PersistentMap<Integer, TableData> tabledata;
	private final Map<Integer, TableData> tabledataUpdates =
			new HashMap<Integer, TableData>();

	public final int num;
	private final ArrayList<TranRead> reads = new ArrayList<TranRead>();
	final Deque<TranWrite> writes = new ArrayDeque<TranWrite>();
	public static final Transaction NULLTRAN = new NullTransaction();

	public static Transaction readonly(Transactions trans,
			PersistentMap<Integer, TableData> tabledata) {
		return new Transaction(trans, true, tabledata);
	}

	public static Transaction readwrite(Transactions trans,
			PersistentMap<Integer, TableData> tabledata) {
		return new Transaction(trans, false, tabledata);
	}

	private Transaction(Transactions trans, boolean readonly,
			PersistentMap<Integer, TableData> tabledata) {
		this.trans = trans;
		this.readonly = readonly;
		t = asof = trans.clock();
		num = trans.nextNum();
		this.tabledata = tabledata;
		trans.add(this);
	}

	// used for cursor setup
	public Transaction(PersistentMap<Integer, TableData> tabledata) {
		this.tabledata = tabledata;
		trans = null;
		readonly = true;
		t = asof = num = 0;
	}

	// used by NullTransaction
	private Transaction() {
		this(null);
	}

	@Override
	public String toString() {
		return "Transaction " + (readonly ? "read " : "update ") +
				num + " time " + t + " asof " + asof;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public boolean isEnded() {
		return ended;
	}

	public long asof() {
		return asof;
	}

	public String conflict() {
		return conflict;
	}

	public TableData getTableData(int tblnum) {
		TableData td = tabledataUpdates.get(tblnum);
		return td != null ? td : tabledata.get(tblnum);
	}

	public void updateTableData(TableData td) {
		verify(!readonly);
		tabledataUpdates.put(td.num, td);
	}

	public TranRead read_act(int tblnum, String index) {
		notEnded();
		TranRead tr = new TranRead(tblnum, index);
		reads.add(tr);
		return tr;
	}

	private void notEnded() {
		if (ended)
			throw new SuException("cannot use ended transaction");
	}

	public void create_act(int tblnum, long adr) {
		verify(! readonly);
		notEnded();
		trans.putCreated(adr, t);
		writes.add(TranWrite.create(tblnum, adr, trans.clock()));
	}

	public boolean delete_act(int tblnum, long adr) {
		verify(! readonly);
		notEnded();
		String c = trans.deleteConflict(tblnum, adr);
		if (!c.equals("")) {
			conflict = c;
			asof = FUTURE;
			return false;
		}
		trans.putDeleted(this, adr, t);
		writes.add(TranWrite.delete(tblnum, adr, trans.clock()));
		return true;
	}

	public void undo_delete_act(int tblnum, long adr) {
		verify(!readonly);
		trans.removeDeleted(this, adr);
		TranWrite tw = writes.removeLast();
		verify(tw.type == TranWrite.Type.DELETE && tw.tblnum == tblnum
				&& tw.off == adr);
	}

	// used by {@link BtreeIndex} to determine if records are visible to a
	// transaction
	public boolean visible(long adr) {
		long ct = trans.createTime(adr);
		if (ct > UNCOMMITTED) {
			if (ct - UNCOMMITTED != t)
				return false;
		} else if (ct > asof)
			return false;

		long dt = trans.deleteTime(adr);
		if (dt > UNCOMMITTED)
			return dt - UNCOMMITTED != t;
		return dt >= asof;
	}

	public void ck_complete() {
		String s = complete();
		if (s != null)
			throw new SuException("transaction commit failed: " + s);
	}

	public String complete() {
		notEnded();
		if (conflict != null) {
			abort();
			return conflict;
		}
		if (!readonly && !writes.isEmpty()) {
			if (!validate_reads()) {
				abort();
				return conflict;
			}
			completeReadwrite();
		}
		trans.remove(this);
		ended = true;
		return null;
	}

	/**
	 * Checks if any of the records this transaction read have been modified
	 * since then. (stale read)
	 * Pretty ugly -
	 *	for each read
	 *		for each final tran with asof > ours
	 *			for each tran write
	 * @return true if all the reads are still valid, false if any conflict
	 */
	private boolean validate_reads() {
		// PERF merge overlapping reads (add org to TranRead.compareTo)
		int cur_tblnum = -1;
		String cur_index = "";
		ImmutableList<Integer> colnums = null;
		int nidxcols = 0;
		Collections.sort(reads);
		for (TranRead tr : reads) {
			if (tr.tblnum != cur_tblnum || ! tr.index.equals(cur_index)) {
				cur_tblnum = tr.tblnum;
				cur_index = tr.index;
				Table tbl = trans.db.getTable(tr.tblnum);
				if (tbl == null)
					continue ;
				colnums = tbl.columns.nums(tr.index);
				nidxcols = colnums.size();
			}
			// crude removal of record address from org & end
			Record from = tr.org;
			if (from.size() > nidxcols)
				from = from.dup().truncate(nidxcols);
			Record to = tr.end;
			if (to.size() > nidxcols)
				to = to.dup().truncate(nidxcols);

			conflict = trans.anyConflicts(asof, tr.tblnum, colnums, from, to,
					tr.index);
			if (conflict != null)
				return false;
		}
		reads.clear(); // no longer needed
		return true;
	}

	private void completeReadwrite() {
		updateTableData();

		int ncreates = 0;
		int ndeletes = 0;
		long commit_time = trans.clock();
		for (TranWrite tw : writes)
			switch (tw.type) {
			case CREATE:
				trans.updateCreated(tw.off, commit_time);
				++ncreates;
				break;
			case DELETE:
				trans.updateDeleted(tw.off, commit_time);
				++ndeletes;
				break;
			default:
				throw SuException.unreachable();
			}
		asof = commit_time;
		trans.addFinal(this);
		writeCommitRecord(ncreates, ndeletes);
	}

	private void updateTableData() {
		for (Map.Entry<Integer, TableData> e : tabledataUpdates.entrySet()) {
			TableData tdNew = e.getValue();
			TableData tdOld = tabledata.get(tdNew.num);
			trans.db.updateTableData(tdNew.num, tdNew.nextfield,
					tdNew.nrecords - tdOld.nrecords,
					tdNew.totalsize - tdOld.totalsize);
		}
	}

	private void writeCommitRecord(int ncreates, int ndeletes) {
		if (ndeletes == 0 && ncreates == 0)
			return;
		final int n = 8 + 4 + 4 + 4 + 4 * (ncreates + ndeletes) + 4;
		ByteBuffer buf = trans.db.adr(trans.db.alloc(n, Mmfile.COMMIT));
		buf.putLong(new Date().getTime());
		buf.putInt(num);
		buf.putInt(ncreates);
		buf.putInt(ndeletes);
		for (TranWrite tw : writes)
			if (tw.type == TranWrite.Type.CREATE)
				buf.putInt(Mmfile.offsetToInt(tw.off));
		for (TranWrite tw : writes)
			if (tw.type == TranWrite.Type.DELETE)
				buf.putInt(Mmfile.offsetToInt(tw.off));
		// include commit in checksum, but don't include checksum itself
		trans.db.checksum(buf, buf.position());
		buf.putInt(trans.db.getChecksum());
		verify(buf.position() == n);
		trans.db.resetChecksum();
	}

	/**
	 * Called by {@link Transactions}. Removes created and deleted records.
	 * Physically removes index entries. ({@link Database.removeRecord} just
	 * calls delete_act.)
	 */
	public void finalization() {
		for (TranWrite tw : writes)
			switch (tw.type) {
			case CREATE:
				trans.removeCreated(tw.off);
				break;
			case DELETE:
				trans.removeDeleted(this, tw.off);
				trans.db.remove_index_entries(tw.tblnum, tw.off);
				break;
			default:
				throw SuException.unreachable();
			}
	}

	public void abort() {
		notEnded();
		if (!readonly)
			abortReadwrite();
		trans.remove(this);
		ended = true;
	}

	private void abortReadwrite() {
		for (TranWrite tw : writes)
			switch (tw.type) {
			case CREATE:
				trans.removeCreated(tw.off);
				trans.db.undoAdd(tw.tblnum, tw.off);
				break;
			case DELETE:
				trans.removeDeleted(this, tw.off);
				trans.db.undoDelete(tw.tblnum, tw.off);
				break;
			default:
				throw SuException.unreachable();
			}
	}

	public void abortIfNotComplete() {
		if (!ended)
			abort();
	}

	public int compareTo(Transaction other) {
		return asof < other.asof ? -1 : asof > other.asof ? +1 : 0;
	}
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Transaction))
			return false;
		return t == ((Transaction) other).t;
	}

	@Override
	public int hashCode() {
		throw new SuException("Transaction hashCode not implemented");
	}

	private static class NullTransaction extends Transaction {
		@Override
		public boolean visible(long adr) {
			return true;
		}
		@Override
		public TranRead read_act(int tblnum, String index) {
			return new TranRead(tblnum, index);
		}
		@Override
		public String complete() {
			ended = true;
			return null;
		}
		@Override
		public void abort() {
			ended = true;
		}
	}
}
