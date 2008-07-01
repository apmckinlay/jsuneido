package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Transactions.FUTURE;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Transaction {
	private final Transactions trans;
	private final boolean readonly;
	protected boolean ended = false;
	private String conflict = "";
	private final long t;
	private long asof;
	private final Deque<TranRead> reads = new ArrayDeque<TranRead>();
	private final Deque<TranWrite> writes = new ArrayDeque<TranWrite>();
	public static final Transaction NULLTRAN = new NullTransaction();

	public Transaction(Transactions trans, boolean readonly, long clock) {
		this.trans = trans;
		this.readonly = readonly;
		t = asof = clock;
	}

	public boolean isReadonly() {
		return readonly;
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
		if ("" != (conflict = trans.deleteConflict(adr))) {
			asof = FUTURE;
			verify(trans.cleanup());
			return false;
			}
		trans.putDeleted(adr, t);
		writes.add(TranWrite.delete(tblnum, adr, trans.clock()));
		return true;
	}

	public boolean visible(long adr) {
		return true;
		// long ct = trans.create_time(adr);
		// if (ct > UNCOMMITTED) {
		// if (ct - UNCOMMITTED != t)
		// return false;
		// } else if (ct > asof)
		// return false;
		//
		// long dt = trans.delete_time(adr);
		// if (dt > UNCOMMITTED)
		// return dt - UNCOMMITTED != t;
		// return dt >= asof;
	}

	public boolean complete() {
		// TODO Auto-generated method stub
		verify(! ended);
		ended = true;
		return true;
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
			super(null, true, 0);
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
}
