/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.SuException.verify;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import suneido.SuException;
import suneido.database.query.Request;
import suneido.immudb.Record.Mode;

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
	public static int loadDatabase(String filename, String dbfilename)
			throws IOException {
		int n = 0;
//		File dbfile = new File(dbfilename);
		new File(dbfilename).delete();
		Database db = Database.create(new MmapFile(dbfilename, "rw"));
		try {
			InputStream fin = new BufferedInputStream(
					new FileInputStream(filename));
			try {
				verifyFileHeader(fin);
				String schema;
				while (null != (schema = readTableHeader(fin))) {
					schema = "create" + schema.substring(6);
					load1(db, fin, schema);
					++n;
				}
			} finally {
				fin.close();
			}
		} finally {
			db.close();
		}
		return n;
	}

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

	private static int loadTableImp(String tablename) throws IOException  {
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
		int i = schema.indexOf(' ', 7);
		String table = schema.substring(7, i);
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
		Btree btree = t.getIndex(table.num, index.colNums);
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
				last = load_data_record(fin, table.num, t, recbuf, n, btree, index.colNums);
				if (first == 0)
					first = last;
			}
			otherIndexes(db, t, table, first, last);
			t.commit();
		} finally {
			t.abortIfNotCommitted();
		}
		return nrecs;
	}

	private static int load_data_record(InputStream fin, int tblnum,
			ExclusiveTransaction t, byte[] recbuf, int n, Btree btree, int[] columns)
			throws IOException {
		verify(fin.read(recbuf, 0, n) == n);

		Record rec = convert(recbuf, n);
		return t.loadRecord(tblnum, rec, btree, columns);
	}

	// convert from cSuneido record format to jSuneido format
	private static Record convert(byte[] recbuf, int n) {
		int mode = recbuf[0];
		mode = mode == 'c' ? Mode.BYTE : mode == 's' ? Mode.SHORT : Mode.INT;
		int nfields = (recbuf[2] & 0xff) + (recbuf[3] << 8);
		assert recbuf[3] < 0x3f;
		recbuf[3] = (byte) (recbuf[3] | (mode << 6));
		switch (mode) {
		case Mode.BYTE:
			for (int i = 0; i < nfields + 1; ++i)
				recbuf[4 + i] -= 2;
			break;
		case Mode.SHORT:
			for (int i = 0; i < nfields + 1; ++i)
				putShort(recbuf, i, getShort(recbuf, i) - 2);
			break;
		case Mode.INT:
			for (int i = 0; i < nfields + 1; ++i)
				putInt(recbuf, i, getInt(recbuf, i) - 2);
			break;
		}
		Record rec = new Record(ByteBuffer.wrap(recbuf, 0, n), 2);
		assert rec.bufSize() == n - 2;
		return rec;
	}

	private static int getShort(byte[] buf, int i) {
		return (buf[4 + i * 2] & 0xff) + ((buf[5 + i * 2] & 0xff) << 8);
	}

	private static void putShort(byte[] buf, int i, int n) {
		buf[4 + i * 2] = (byte) (n & 0xff);
		buf[5 + i * 2] = (byte) ((n >> 8) & 0xff);
	}

	private static int getInt(byte[] buf, int i) {
		return (buf[4 + i * 4] & 0xff) +
				((buf[5 + i * 4] & 0xff) << 8) +
				((buf[6 + i * 4] & 0xff) << 16) +
				((buf[7 + i * 4] & 0xff) << 24);
	}

	private static void putInt(byte[] buf, int i, int n) {
		buf[4 + i * 4] = (byte) (n & 0xff);
		buf[5 + i * 4] = (byte) ((n >> 8) & 0xff);
		buf[6 + i * 4] = (byte) ((n >> 16) & 0xff);
		buf[7 + i * 4] = (byte) ((n >> 24) & 0xff);
	}

	private static void otherIndexes(Database db, ExclusiveTransaction t,
			Table table, int first, int last) {
		if (first == 0)
			return; // no data
		t.saveBtrees();
		Iterator<Index> iter = table.indexes.iterator();
		iter.next(); // skip first index (already built)
		while (iter.hasNext()) {
			Index index = iter.next();
			Btree btree = t.getIndex(table.num, index.colNums);
			otherIndex(db, btree, index.colNums, first, last);
			t.saveBtrees();
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

	public static void main(String[] args) throws IOException  {
//		loadDatabasePrint("database.su", "dbload.db");

//		int n = loadTable("gl_transactions");
//		System.out.println("loaded " + n + " records into gl_transactions");

		int n = loadDatabase("database.su", "immu.db");
		System.out.println("loaded " + n + " tables into immu.db");

		DbCheck.checkPrintExit("immu.db");
	}

}
