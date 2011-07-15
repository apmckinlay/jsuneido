/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import suneido.database.BtreeIndex;
import suneido.database.Index;
import suneido.database.Record;

public interface Transaction {

	boolean isReadonly();

	boolean isReadWrite();

	boolean isEnded();

	long asof();

	String conflict();

	boolean tableExists(String table);

	Table ck_getTable(String tablename);

	Table getTable(String tablename);

	Table ck_getTable(int tblnum);

	Table getTable(int tblnum);

	void deleteTable(Table table);

	int nrecords(int tblnum);
	long totalsize(int tblnum);

	BtreeIndex getBtreeIndex(Index index);

	BtreeIndex getBtreeIndex(int tblnum, String columns);

	void abortIfNotComplete();

	void abort();

	void ck_complete();

	String complete();

	String getView(String viewname);

	void addRecord(String table, Record r);

	long updateRecord(long recadr, Record rec);

	void removeRecord(long off);

	Record input(long adr);

	// used by Library
	Record lookup(int tblnum, String index, Record key);

	void callTrigger(Table table, Record oldrec, Record newrec);

	int num();

	Record fromRef(Object ref);

	HistoryIterator historyIterator(int tblnum);

}