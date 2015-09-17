/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.List;

import suneido.database.query.Request;
import suneido.intfc.database.IndexIter;

class DbCompact {
	private final Database oldDB;
	private final Database newDB;
	private ReadTransaction rt;

	static int compact(Database olddb, Database newdb) {
		return new DbCompact(olddb, newdb).compact();
	}

	private DbCompact(Database olddb, Database newdb) {
		this.oldDB = olddb;
		this.newDB = newdb;
	}

	private int compact() {
		return copy();
	}

	private int copy() {
		rt = oldDB.readTransaction();
		copySchema();
		return copyData() + 1; // + 1 for views
	}

	private void copySchema() {
		copyTable("views");
		IndexIter iter = rt.iter(Bootstrap.TN.TABLES, "tablename");
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			String tablename = r.getString(Table.TABLE);
			if (! Database.isSystemTable(tablename))
				createTable(tablename);
		}
	}

	private void createTable(String tablename) {
		String schema = rt.getTable(tablename).schema();
		Request.execute(newDB, "create " + tablename + schema);
	}

	private int copyData() {
		IndexIter iter = rt.iter(Bootstrap.TN.TABLES, "tablename");
		int n = 0;
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			String tablename = r.getString(Table.TABLE);
			if (! Database.isSystemTable(tablename)) {
				copyTable(tablename);
				++n;
			}
		}
		return n;
	}

	// TODO create each index by reading via the same index in the source
	// so that keys are added optimally in order
	private void copyTable(String tablename) {
		Table oldtable = rt.ck_getTable(tablename);
		List<String> fields = oldtable.getFields();
		boolean squeeze = DbDump.needToSqueeze(rt, oldtable.num, fields);
		BulkTransaction t = newDB.bulkTransaction();
		try {
			int first = 0;
			int last = 0;
			Table newtable = t.ck_getTable(tablename);
			IndexIter iter = rt.iter(oldtable.num, null);
			for (iter.next(); ! iter.eof(); iter.next()) {
				DataRecord r = rt.input(iter.keyadr());
				if (squeeze)
					r = DbDump.squeezeRecord(r, fields).build();
				last = t.loadRecord(newtable.num, r);
				if (first == 0)
					first = last;
			}
			DbLoad.createIndexes(t, newtable, first, last);
			t.ck_complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

//	public static void main(String[] args) throws InterruptedException {
//		Database dbin = (Database) dbpkg.openReadonly("/test/sample/suneido.db");
//		Database dbout = (Database) dbpkg.create("immu.compact");
//		Stopwatch sw = Stopwatch.createStarted();
//		int n = compact(dbin, dbout);
//		System.out.println("compacted " + n + " tables " + "in " + sw);
//	}

}
