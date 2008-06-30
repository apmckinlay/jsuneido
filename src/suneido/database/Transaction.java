package suneido.database;

import static suneido.Suneido.verify;

/**
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Transaction {
	private final boolean readonly;
	protected boolean ended = false;
	public static final Transaction NULLTRAN = new NullTransaction();
	
	private Transaction(boolean readonly) {
		this.readonly = readonly;
	}

	public static Transaction readonly() {
		// TODO Auto-generated method stub
		return new Transaction(true);
	}
	
	public static Transaction readwrite() {
		// TODO Auto-generated method stub
		return new Transaction(false);
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
		// TODO Auto-generated method stub
		verify(! readonly);
		verify(! ended);
	}
	
	public void delete_act(int tblnum, long adr) {
		// TODO Auto-generated method stub
		verify(! readonly);
		verify(! ended);
	}

	public boolean visible(long adr) {
		// TODO
		return true;
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

	private static class NullTransaction extends Transaction {
		private NullTransaction() {
			super(true);
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
