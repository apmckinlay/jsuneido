package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Transactions.FUTURE;
import static suneido.database.Transactions.UNCOMMITTED;

import java.util.ArrayDeque;
import java.util.Deque;

import suneido.SuException;

/**
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Transaction implements Comparable<Transaction> {
	private final Transactions trans;
	private final boolean readonly;
	protected boolean ended = false;
	private String conflict = "";
	private final long t;
	private long asof;
	public final int num;
	private final Deque<TranRead> reads = new ArrayDeque<TranRead>();
	private final Deque<TranWrite> writes = new ArrayDeque<TranWrite>();
	public static final Transaction NULLTRAN = new NullTransaction();

	public Transaction(Transactions trans, boolean readonly, long clock, int num) {
		this.trans = trans;
		this.readonly = readonly;
		t = asof = clock;
		this.num = num;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public long asof() {
		return asof;
	}

	public TranRead read_act(int tblnum, String index) {
		// TODO
		verify(! ended);
		return new TranRead(tblnum, index);
	}

	public void create_act(int tblnum, long adr) {
		verify(! readonly);
		verify(! ended);
		trans.putCreated(adr, t);
		writes.add(TranWrite.create(tblnum, adr, trans.clock()));
	}

	public boolean delete_act(int tblnum, long adr) {
		verify(! readonly);
		verify(! ended);
		String c;
		if ("" != (c = trans.deleteConflict(adr))) {
			conflict = c;
			asof = FUTURE;
			return false;
			}
		trans.putDeleted(adr, t);
		writes.add(TranWrite.delete(tblnum, adr, trans.clock()));
		return true;
	}

	public boolean visible(long adr) {
		long ct = trans.create_time(adr);
		if (ct > UNCOMMITTED) {
			if (ct - UNCOMMITTED != t)
				return false;
		} else if (ct > asof)
			return false;

		long dt = trans.delete_time(adr);
		if (dt > UNCOMMITTED)
			return dt - UNCOMMITTED != t;
		return dt >= asof;
	}

	public boolean complete() {
		verify(! ended);
		if (conflict != "") {
			abort();
			return false;
		}
		if (!readonly && !writes.isEmpty()) {
			if (!validate_reads()) {
				abort();
				return false;
			}
			commit();
		}
		trans.completed(this);
		ended = true;
		return true;
	}

	private boolean validate_reads() {
		// TODO Auto-generated method stub
		reads.clear(); // no longer needed
		return true;
	}

	private void commit() {
		int ncreates = 0;
		int ndeletes = 0;
		long commit_time = trans.clock();
		for (TranWrite tw : writes) {
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
		}
		asof = commit_time;
		trans.addFinal(this);
		writeCommitRecord(ncreates, ndeletes);
	}

	private void writeCommitRecord(int ncreates, int ndeletes) {
		// TODO Auto-generated method stub
	}

	public boolean finalization() {
		int n = 0;
		for (TranWrite tw : writes) {
			try {
				switch (tw.type) {
				case CREATE:
					trans.removeCreated(tw.off);
					break;
				case DELETE:
					trans.removeDeleted(tw.off);
					trans.db.remove_index_entries(tw.tblnum, tw.off);
					break;
				default:
					throw SuException.unreachable();
				}
				++n;
			} finally {
				--n;
			}
		}
		return n == 0;
	}

	public void abort() {
		verify(! ended);
		ended = true;
		// TODO
	}
	public void abortIfNotComplete() {
		if (!ended)
			abort();
	}

	public String conflict() {
		return conflict;
	}

	private static class NullTransaction extends Transaction {
		private NullTransaction() {
			super(null, true, 0, 0);
		}
		@Override
		public boolean visible(long adr) {
			return true;
		}
		@Override
		public TranRead read_act(int tblnum, String index) {
			return new TranRead(tblnum, index);
		}
		@Override
		public boolean complete() {
			ended = true;
			return true;
		}
		@Override
		public void abort() {
			ended = true;
		}
	}

	public int compareTo(Transaction other) {
		return asof < other.asof ? -1 : asof > other.asof ? +1 : 0;
	}
}
