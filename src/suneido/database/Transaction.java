package suneido.database;

/**
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Transaction {
	public static final Transaction NULLTRAN = new NullTransaction();
	
	private Transaction() {
	}

	public static Transaction readonly() {
		// TODO Auto-generated method stub
		return new Transaction();
	}
	
	public TranRead read_act(int tblnum, String index) {
		// TODO
		return new TranRead(tblnum, index);
	}
	
	public void create_act(int tblnum, long adr) {
		// TODO Auto-generated method stub
	}
	
	public void delete_act(int tblnum, long adr) {
		// TODO Auto-generated method stub
	}

	public boolean visible(long adr) {
		// TODO
		return true;
	}

	public void complete() {
		// TODO Auto-generated method stub
	}

	private static class NullTransaction extends Transaction {
		public boolean visible(long adr) {
			return true;
		}
		public TranRead read_act(int tblnum, String index) {
			return new TranRead(tblnum, index);
		}
		public void complete() {
		}
	}
}
