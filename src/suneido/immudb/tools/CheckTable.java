/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb.tools;

import java.util.concurrent.Callable;

import suneido.immudb.*;
import suneido.immudb.schema.Index;
import suneido.immudb.schema.Table;

public class CheckTable implements Callable<String> {
	final Database db;
	final String tableName;
	String details = "";

	public CheckTable(Database db, String tableName) {
		this.db = db;
		this.tableName = tableName;
	}

	public static void check(Database db, String tableName) {
		String s = new CheckTable(db, tableName).call();
		if (! s.isEmpty())
			throw new RuntimeException("CheckTable " + tableName + " " + s);
	}

	@Override
	public String call() {
		ReadTransaction t = db.readTran();
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
			Btree btree = t.getIndex(table.num, index.colNums);
			Btree.Iter iter = btree.iterator();
			Record prevkey = null;
			for (iter.next(); !iter.eof(); iter.next()) {
				Record key = iter.cur();
				Record strippedKey = new RecordBuilder().addPrefix(key, key.size() - 1).build();
				if (index.isKey())// || (index.unique && ! BtreeIndex.isEmpty(strippedKey)))
					if (strippedKey.equals(prevkey)) {
						details += tablename + ": duplicate in " + index + "\n";
						return false;
					}
				prevkey = strippedKey;
				int adr = Btree.getAddress(key);
				Record rec = t.getrec(adr);
				if (first_index)
					if (!checkRecord(tablename, rec))
						return false;
				Record reckey = IndexedData.key(rec, index.colNums, adr);
				if (! key.equals(reckey)) {
					details += tablename + ": key mismatch in " + index + "\n";
					return false;
				}
				++nrecords;
				totalsize += rec.length();
				if (rec.size() > maxfields)
					maxfields = rec.size();
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
		if (tablename.equals("tables") || tablename.equals("indexes"))
			maxfields -= 1; // allow for the padding
		if (maxfields > ti.nextfield) {
			details += tablename + ": nextfield mismatch: maxfields "
					+ maxfields + " > nextfield " + ti.nextfield + "\n";
			return false;
		}
		return true;
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