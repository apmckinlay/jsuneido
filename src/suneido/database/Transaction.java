package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Transactions.*;

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
	private long t;
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
		trans.create_act(tblnum, adr, t);
	}
	
	public boolean delete_act(int tblnum, long adr) {
		verify(! readonly);
		verify(! ended);
		if (! trans.delete_act(tblnum, adr, t)) {
			conflict = "deleted"; // TODO write_conflict(tblnum, adr, td);
			asof = FUTURE;
			return false;
		}
		return true;
	}

	public boolean visible(long adr) {
		long ct = trans.create_time(adr);
		if (ct > UNCOMMITTED)
			{
			if (ct - UNCOMMITTED != t)
				return false;
			}
		else if (ct > asof)
			return false;

		long dt = trans.delete_time(adr);
		if (dt > UNCOMMITTED)
			return dt - UNCOMMITTED != t;
		return dt >= asof;
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
	
	public String conflict() {
		return conflict;
	}

	private static class NullTransaction extends Transaction {
		private NullTransaction() {
			super(null, true, 0);
		}
		public boolean visible(long adr) {
			return true;
		}
		public TranRead read_act(int tblnum, String index) {
			return new TranRead(tblnum, index);
		}
		public boolean complete() {
			ended = true;
			return true;
		}
		public void abort() {
			ended = true;
		}
	}
}
