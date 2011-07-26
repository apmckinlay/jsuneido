/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import suneido.language.Pack;

class CheckTable implements Callable<String> {
	private final Database db;
	private final String tablename;
	private String details = "";

	CheckTable(Database db, String tablename) {
		this.db = db;
		this.tablename = tablename;
	}

	@Override
	public String call() {
		Transaction t = db.readonlyTran();
		try {
			checkTable(t, tablename);
			return details;
		} finally {
			t.complete();
		}
	}

	private boolean checkTable(Transaction t, String tablename) {
		boolean first_index = true;
		Table table = t.getTable(tablename);
		TableData td = t.getTableData(table.num);
		int maxfields = 0;
		for (Index index : table.indexes) {
			int nrecords = 0;
			long totalsize = 0;
			BtreeIndex bti = t.getBtreeIndex(index);
			BtreeIndex.Iter iter = bti.iter();
			Record prevkey = null;
			for (iter.next(); !iter.eof(); iter.next()) {
				Record key = iter.cur().key;
				Record strippedKey = BtreeIndex.stripAddress(key);
				if (bti.iskey || (bti.unique && !BtreeIndex.isEmpty(strippedKey)))
					if (strippedKey.equals(prevkey)) {
						details += tablename + ": duplicate in " + index + "\n";
						return false;
					}
				prevkey = strippedKey;
				Record rec = db.input(iter.keyoff());
				if (first_index)
					if (!checkRecord(tablename, rec))
						return false;
				Record reckey = rec.project(index.colnums, iter.keyoff());
				if (!key.equals(reckey)) {
					details += tablename + ": key mismatch in " + index + "\n";
					return false;
				}
				++nrecords;
				totalsize += rec.packSize();
				if (rec.size() > maxfields)
					maxfields = rec.size();
			}
			if (nrecords != td.nrecords) {
				details += tablename + ": record count mismatch: " +
						index + " " + nrecords +
						" != tables " + td.nrecords + "\n";
				return false;
			}
			if (totalsize != td.totalsize) {
				details += tablename + ": data size mismatch: " +
						index + " " + totalsize +
						" != tables " + td.totalsize + "\n";
				return false;
			}
		}
		if (td.nextfield <= table.maxColumnNum()) {
			details += tablename + ": nextfield mismatch: nextfield "
					+ td.nextfield + " <= max column# " + table.maxColumnNum() + "\n";
			return false;
		}
		if (tablename.equals("tables") || tablename.equals("indexes"))
			maxfields -= 1; // allow for the padding
		if (maxfields > td.nextfield) {
			details += tablename + ": nextfield mismatch: maxfields "
					+ maxfields + " > nextfield " + td.nextfield + "\n";
			return false;
		}
		return true;
	}

	private boolean checkRecord(String tablename, Record rec) {
		for (ByteBuffer buf : rec)
			try {
				Pack.unpack(buf);
			} catch (Throwable e) {
				details += tablename + ": " + e + "\n";
				return false;
			}
		return true;
	}

}