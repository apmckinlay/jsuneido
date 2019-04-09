/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public abstract class Transaction {

	public abstract boolean isReadonly();

	public abstract boolean isReadWrite();

	public abstract boolean isEnded();

	public abstract String conflict();

	public abstract boolean tableExists(String table);

	public abstract Table ck_getTable(String tablename);

	public abstract Table getTable(String tablename);

	public abstract Table ck_getTable(int tblnum);

	public abstract Table getTable(int tblnum);

	public abstract int tableCount(int tblnum);
	public abstract long tableSize(int tblnum);
	public abstract int indexSize(int tblnum, String columns);
	public abstract int keySize(int tblnum, String columns);
	public abstract float rangefrac(int tblnum, String columns, Record from, Record to);

	public abstract void abortIfNotComplete();

	public abstract void abort();

	public abstract void ck_complete();

	/** @return null if successful, an error string if there is a conflict */
	public abstract String complete();

	public abstract String getView(String viewname);

	public abstract void addRecord(String table, Record r);

	public enum Blocking { BLOCK, NO_BLOCK }

	public abstract int updateRecord(int recadr, Record rec, Blocking blocking);
	public int updateRecord(int recadr, Record rec) {
		return updateRecord(recadr, rec, Blocking.BLOCK);
	}

	public abstract int updateRecord(int tblnum, Record oldrec, Record newrec, Blocking blocking);
	public int updateRecord(int tblnum, Record oldrec, Record newrec) {
		return updateRecord(tblnum, oldrec, newrec, Blocking.BLOCK);
	}

	public abstract void removeRecord(int recadr);
	public abstract int removeRecord(int tblnum, Record rec);

	public abstract Record input(int adr);

	// used by Library
	public abstract Record lookup(int tblnum, String index, Record key);

	public abstract void callTrigger(Table table, Record oldrec, Record newrec);

	public abstract int num();

	public abstract Record fromRef(Object ref);

	public abstract HistoryIterator historyIterator(int tblnum);

	public abstract IndexIter iter(int tblnum, String columns);
	public abstract IndexIter iter(int tblnum, String columns, Record org, Record end);
	public abstract IndexIter iter(int tblnum, String columns, IndexIter iter);

	public abstract boolean isAborted();

	public int readCount() {
		return 0;
	}
	public int writeCount() {
		return 0;
	}

}
