/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.tools;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static suneido.SuException.verify;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import suneido.SuException;
import suneido.database.immudb.*;
import suneido.database.immudb.query.Request;
import suneido.database.immudb.schema.Index;
import suneido.database.immudb.schema.Table;

public class DbLoad {

//	public static void loadDatabasePrint(String filename, String dbfilename)
//			throws InterruptedException {
//		File tempfile = FileUtils.tempfile();
//		if (! DbTools.runWithNewJvm("-load:" + tempfile))
//			throw new SuException("failed to load: " + filename);
//		FileUtils.renameWithBackup(tempfile, dbfilename);
//		System.out.println("loaded " + filename	+ " into new " + dbfilename);
//	}
//
//	public static int load2(String filename, String dbfilename) {
//		try {
//			return loadDatabase(filename, dbfilename);
//		} catch (Throwable e) {
//			throw new SuException("load " + filename + " failed", e);
//		}
//	}
//
//	public static int loadDatabase(String filename, String dbfilename)
//			throws Throwable {
//		int n = 0;
//		File dbfile = new File(dbfilename);
//		try {
//			TheDb.set(new Database(dbfile, Mode.CREATE));
//			InputStream fin = new BufferedInputStream(
//					new FileInputStream(filename));
//			try {
//				verifyFileHeader(fin);
//				String schema;
//				try {
//					TheDb.db().setLoading(true);
//					while (null != (schema = readTableHeader(fin))) {
//						schema = "create" + schema.substring(6);
//						load1(fin, schema);
//						++n;
//					}
//				} finally {
//					TheDb.db().setLoading(false);
//				}
//			} finally {
//				fin.close();
//			}
//		} finally {
//			TheDb.db().close();
//		}
//		return n;
//	}

	public static void loadTablePrint(String tablename) {
		int n = loadTable(tablename);
		System.out.println("loaded " + n + " records into suneido.db");
	}

	public static int loadTable(String tablename) {
		try {
			return loadTableImp(tablename);
		} catch (Throwable e) {
			throw new SuException("load " + tablename + " failed", e);
		}
	}

	private static int loadTableImp(String tablename) throws Throwable {
		if (tablename.endsWith(".su"))
			tablename = tablename.substring(0, tablename.length() - 3);
//		File dbfile = new File("suneido.db");
//		Mode mode = dbfile.exists() ? Mode.OPEN : Mode.CREATE;
//		TheDb.set(new Database(dbfile, mode));
new File("immu.db").delete();
		Database db = Database.create(new MmapFile("immu.db", "rw"));
		try {
			InputStream fin = new BufferedInputStream(
					new FileInputStream(tablename + ".su"));
			try {
				verifyFileHeader(fin);
				String schema = readTableHeader(fin);
				if (schema == null)
					throw new SuException("not a valid dump file");
				schema = "create " + tablename + schema.substring(6);
				return load1(db, fin, schema);
			} finally {
				fin.close();
			}
		} finally {
			db.close();
		}
	}

	private static void verifyFileHeader(InputStream fin)
			throws IOException {
		String s = getline(fin);
		if (s == null || !s.startsWith("Suneido dump"))
			throw new SuException("not a valid dump file");
	}

	private static String readTableHeader(InputStream fin)
			throws IOException {
		String schema = getline(fin);
		if (schema == null)
			return null;
		verify(schema.startsWith("====== "));
		return schema;
	}

	private static int load1(Database db, InputStream fin, String schema)
			throws IOException {
		int n = schema.indexOf(' ', 7);
		String table = schema.substring(7, n);

		if (! "views".equals(table)) {
//			Schema.removeTable(TheDb.db(), table);
			Request.execute(db, schema);
		}
		return load_data(db, fin, table);
	}

	private static int load_data(Database db, InputStream fin, String tablename)
			throws IOException {
		int nrecs = 0;
		byte[] buf = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
		byte[] recbuf = new byte[4096];
		ExclusiveTransaction t = db.exclusiveTran();
		Table table = t.getTable(tablename);
		Index index = table.indexes.first();
		Btree btree = t.getIndex(table.num, index.columns);
		try {
			int first = 0;
			int last = 0;
			for (;; ++nrecs) {
				verify(fin.read(buf) == buf.length);
				int n = bb.getInt(0);
				if (n == 0)
					break;
				if (n > recbuf.length)
					recbuf = new byte[Math.max(n, 2 * recbuf.length)];
				last = load_data_record(fin, table.num, t, recbuf, n, btree, index.columns);
				if (first == 0)
					first = last;
			}
			otherIndexes(db, t, table, first, last);
		} finally {
			t.commit();
		}
		return nrecs;
	}

	private static int load_data_record(InputStream fin, int tblnum,
			ExclusiveTransaction t, byte[] recbuf, int n, Btree btree, int[] columns)
			throws IOException {
		verify(fin.read(recbuf, 0, n) == n);
		Record rec = new Record(ByteBuffer.wrap(recbuf, 0, n));
		return t.loadRecord(tblnum, rec, btree, columns);
	}

	private static void otherIndexes(Database db, ExclusiveTransaction t,
			Table table, int first, int last) {
		t.saveBtrees();
		Iterator<Index> iter = table.indexes.iterator();
		iter.next(); // skip first index (already built)
		while (iter.hasNext()) {
			Index index = iter.next();
			Btree btree = t.getIndex(table.num, index.columns);
			otherIndex(db, btree, index.columns, first, last);
System.out.println("built " + index);
		}
	}

	private static void otherIndex(Database db, Btree btree, int[] columns,
			int first, int last) {
		StoredRecordIterator iter = new StoredRecordIterator(db.stor, first, last);
		while (iter.hasNext()) {
			int adr = iter.nextAdr();
			Record rec = iter.next();
			Record key = IndexedData.key(rec, columns, adr);
			btree.add(key);
		}
	}

	private static String getline(InputStream fin) throws IOException {
		StringBuilder sb = new StringBuilder();
		int c;
		while ('\n' != (c = fin.read())) {
			if (c == -1)
				return null;
			sb.append((char) c);
		}
		return sb.toString();
	}

	public static void main(String[] args) throws InterruptedException {
//		loadDatabasePrint("database.su", "dbload.db");

		int n = loadTable("gl_transactions");
		System.out.println("loaded " + n + " records into gl_transactions");
		Database db = Database.open(new MmapFile("immu.db", "r"));
//		ReadTransaction t = db.readTran();
//		Table tbl = t.getTable("gl_transactions");
//		System.out.println(tbl.schema());
//		System.out.println(tbl);
//		TableInfo ti = t.getTableInfo(tbl.num);
//		System.out.println(ti);
		System.out.println("checking...");
		assertThat(new CheckTable(db, "gl_transactions").call(), is(""));
		System.out.println("...done");
	}

}
