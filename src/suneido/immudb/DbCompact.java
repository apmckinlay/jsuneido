/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Verify.verifyEquals;

import java.util.List;

import suneido.DbTools;
import suneido.database.query.Request;
import suneido.intfc.database.IndexIter;

class DbCompact {
//	private final String dbfilename;
//	private final String tempfilename;
	private final Database oldDB;
	private final Database newDB;
	private ReadTransaction rt;

//	static void compactPrint(String dbfilename)
//			throws InterruptedException {
//		File tempfile = FileUtils.tempfile();
//		compact2(dbfilename, tempfile.getPath());
//		FileUtils.renameWithBackup(tempfile, dbfilename);
//	}
//
//	private static void compact2(String dbfilename, String tempfilename) {
//		Status status = DbCheck.checkPrint(dbfilename);
//		if (status != Status.OK)
//			throw new SuException("Compact FAILED " + dbfilename + " " + status);
//		System.out.println("Compacting " + dbfilename);
//		int n = new DbCompact(dbfilename, tempfilename).compact();
//		System.out.println(dbfilename + " compacted " + n + " tables");
//	}

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
		verifyEquals(schema, newDB.getSchema(tablename));
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
		boolean squeeze = needToSqueeze(fields);
		BulkTransaction t = newDB.bulkTransaction();
		try {
			int first = 0;
			int last = 0;
			Table newtable = t.ck_getTable(tablename);
			IndexIter iter = rt.iter(oldtable.num, null);
			for (iter.next(); ! iter.eof(); iter.next()) {
				Record r = rt.input(iter.keyadr());
				if (squeeze)
					r = squeezeRecord(r, fields);
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

	private static boolean needToSqueeze(List<String> fields) {
		return fields.indexOf("-") != -1;
	}

	private static Record squeezeRecord(Record rec, List<String> fields) {
		RecordBuilder rb = new RecordBuilder();
		int i = 0;
		for (String f : fields) {
			if (!f.equals("-"))
				rb.add(rec.getRaw(i));
			++i;
		}
		return rb.build();
	}

	public static void main(String[] args) throws InterruptedException {
		DbTools.compactPrintExit(DatabasePackage.dbpkg, "immu.db");
	}

}
