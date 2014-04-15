/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.concurrent.Callable;

import suneido.intfc.database.IndexIter;

/**
 * Check records and indexes with dbinfo for a single table.
 * Run in parallel by {@link DbCheck}.
 */
class CheckTable implements Callable<String> {
	private final Database db;
	private final String tableName;
	private String details = "";

	CheckTable(Database db, String tableName) {
		this.db = db;
		this.tableName = tableName;
	}

	static void check(Database db, String tableName) {
		String s = new CheckTable(db, tableName).call();
		if (! s.isEmpty())
			throw new RuntimeException("CheckTable " + tableName + " " + s);
	}

	/** @return "" if ok, otherwise a description of problems */
	@Override
	public String call() {
		ReadTransaction t = db.readTransaction();
		try {
			checkTable(t);
		} finally {
			t.complete();
		}
		return details;
	}

	private boolean checkTable(ReadTransaction t) {
		Table table = t.getTable(tableName);
		TableInfo ti = t.getTableInfo(table.num);
		if (! checkIndexes(t, table, ti))
			return false;
		if (ti.nextfield <= table.maxColumnNum()) {
			details += tableName + ": nextfield mismatch:" +
					" nextfield " + ti.nextfield +
					" should not be <= max column# " + table.maxColumnNum() + "\n";
			return false;
		}
		return true;
	}

	private boolean checkIndexes(ReadTransaction t, Table table, TableInfo ti) {
		boolean first_index = true;
		for (Index index : table.indexes) {
			if (! checkIndex(t, table, ti, index, first_index))
				return false;
			first_index = false;
		}
		return true;
	}

	private boolean checkIndex(ReadTransaction t, Table table,
			TableInfo ti, Index index, boolean first_index) {
		int nrecords = 0;
		long totalsize = 0;
		TranIndex btree = t.getIndex(table.num, index.colNums);
		btree.check();
		IndexIter iter = btree.iterator();
		Record prevkey = null;
		for (iter.next(); !iter.eof(); iter.next()) {
			Record key = (Record) iter.curKey();
			if (prevkey != null && isUnique(index, key) && key.equals(prevkey)) {
				details += tableName + ": duplicate in " + index + " " + key + "\n";
				return false;
			}
			prevkey = key;
			int adr = iter.keyadr();
			Record rec = t.input(adr);
			if (first_index && ! checkRecord(rec))
				return false;
			BtreeKey reckey = IndexedData.key(rec, index.colNums, adr);
			if (! key.equals(reckey.key)) {
				details += tableName + ": key mismatch in " + index + "\n";
				return false;
			}
			++nrecords;
			totalsize += rec.bufSize();
			if (rec.size() > ti.nextfield) {
				details += tableName + ": nextfield mismatch: rec size "
						+ rec.size() + " should not be > nextfield " + ti.nextfield + "\n";
				return false;
			}
		}
		if (nrecords != ti.nrows()) {
			details += tableName + ": record count mismatch: " +
					index + " " + nrecords +
					" should = tables " + ti.nrows() + "\n";
			return false;
		}
		if (totalsize != ti.totalsize()) {
			details += tableName + ": data size mismatch: " +
					index + " " + totalsize +
					" should = tables " + ti.totalsize() + "\n";
			return false;
		}
		return true;
	}

	private static boolean isUnique(Index index, Record key) {
		return index.isKey() || (index.unique && ! IndexedData.isEmptyKey(key));
	}

	private boolean checkRecord(Record rec) {
		for (int i = 0; i < rec.size(); ++i)
			try {
				rec.get(i);
			} catch (Throwable e) {
				details += tableName + ": " + e + "\n";
				return false;
			}
		return true;
	}

}