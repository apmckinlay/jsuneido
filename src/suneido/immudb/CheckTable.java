/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.BtreeNode.adr;

import java.util.concurrent.Callable;

import suneido.intfc.database.IndexIter;

class CheckTable implements Callable<String> {
	final Database db;
	final String tableName;
	String details = "";

	CheckTable(Database db, String tableName) {
		this.db = db;
		this.tableName = tableName;
	}

	static void check(Database db, String tableName) {
		String s = new CheckTable(db, tableName).call();
		if (! s.isEmpty())
			throw new RuntimeException("CheckTable " + tableName + " " + s);
	}

	@Override
	public String call() {
		ReadTransaction t = db.readTransaction();
		checkTable(t, tableName);
		return details;
	}

	private boolean checkTable(ReadTransaction t, String tablename) {
		boolean first_index = true;
		Table table = t.getTable(tablename);
		TableInfo ti = t.getTableInfo(table.num);
		int maxfields = 0;
		for (Index index : table.indexes) {
			int nrecords = 0;
			long totalsize = 0;
			TranIndex btree = t.getIndex(table.num, index.colNums);
			btree.check();
			IndexIter iter = btree.iterator();
			Record prevkey = null;
			for (iter.next(); !iter.eof(); iter.next()) {
				Record key = (Record) iter.curKey();
				if (prevkey != null && isUnique(index, key) &&
					key.prefixEquals(prevkey, key.size() - 1)) {
					details += tablename + ": duplicate in " + index + " " + key + "\n";
					return false;
				}
				prevkey = key;
				int adr = adr(key);
				Record rec = t.input(adr);
				if (first_index)
					if (!checkRecord(tablename, rec))
						return false;
				Record reckey = IndexedData.key(rec, index.colNums, adr);
				if (! key.equals(reckey)) {
					details += tablename + ": key mismatch in " + index + "\n";
					return false;
				}
				++nrecords;
				totalsize += rec.bufSize();
				if (rec.size() > ti.nextfield) {
					details += tablename + ": nextfield mismatch: maxfields "
							+ maxfields + " > nextfield " + ti.nextfield + "\n";
					return false;
				}
			}
			if (nrecords != ti.nrows()) {
				details += tablename + ": record count mismatch: " +
						index + " " + nrecords +
						" != tables " + ti.nrows() + "\n";
				return false;
			}
			if (totalsize != ti.totalsize()) {
				details += tablename + ": data size mismatch: " +
						index + " " + totalsize +
						" != tables " + ti.totalsize() + "\n";
				return false;
			}
		}
		if (ti.nextfield <= table.maxColumnNum()) {
			details += tablename + ": nextfield mismatch: nextfield "
					+ ti.nextfield + " <= max column# " + table.maxColumnNum() + "\n";
			return false;
		}
		return true;
	}

	private static boolean isUnique(Index index, Record key) {
		return index.isKey() || (index.unique && ! IndexedData.isEmptyKey(key));
	}

	private boolean checkRecord(String tablename, Record rec) {
		for (int i = 0; i < rec.size(); ++i)
			try {
				rec.get(i);
			} catch (Throwable e) {
				details += tablename + ": " + e + "\n";
				return false;
			}
		return true;
	}

}