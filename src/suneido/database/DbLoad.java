/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.util.FileUtils.fullRead;
import static suneido.util.FileUtils.getline;
import static suneido.util.FileUtils.readInt;
import static suneido.util.Verify.verify;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

import suneido.DbTools;
import suneido.SuException;
import suneido.database.query.Request;

class DbLoad {

	static int loadDatabase(Database db, ReadableByteChannel in) {
		try {
			verifyFileHeader(in);
			String schema;
			try {
				int n = 0;
				db.setLoading(true);
				while (null != (schema = readTableHeader(in))) {
					schema = "create" + schema.substring(6);
					load1(db, in, schema);
					++n;
				}
				return n;
			} finally {
				db.setLoading(false);
			}
		} catch (Exception e) {
			throw new RuntimeException("load failed", e);
		}
	}

	static int loadTable(Database db, String tablename, ReadableByteChannel in) {
		try {
			verifyFileHeader(in);
			String schema = readTableHeader(in);
			if (schema == null)
				throw new SuException("not a valid dump file");
			schema = "create " + tablename + schema.substring(6);
			try {
				db.setLoading(true);
				return load1(db, in, schema);
			} finally {
				db.setLoading(false);
			}
		} catch (Exception e) {
			throw new RuntimeException("load table failed", e);
		}
	}

	private static void verifyFileHeader(ReadableByteChannel in)
			throws IOException {
		String s = getline(in);
		if (s == null || ! s.startsWith("Suneido dump"))
			throw new SuException("not a valid dump file");
	}

	private static String readTableHeader(ReadableByteChannel in)
			throws IOException {
		String schema = getline(in);
		if (schema == null)
			return null;
		verify(schema.startsWith("====== "));
		return schema;
	}

	private static int load1(Database db, ReadableByteChannel in, String schema) throws IOException {
		int n = schema.indexOf(' ', 7);
		String table = schema.substring(7, n);

		if (!"views".equals(table)) {
			Schema.dropTable(db, table);
			Request.execute(db, schema);
		}
		return load_data(db, in, table);
	}

	private static int load_data(Database db, ReadableByteChannel in, String tablename) throws IOException {
		int nrecs = 0;
		ByteBuffer intbuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer recbuf = ByteBuffer.allocate(4096);
		Transaction tran = db.readwriteTran();
		try {
			for (;; ++nrecs) {
				int n = readInt(in, intbuf);
				if (n == 0)
					break;
				if (n > recbuf.capacity())
					recbuf = ByteBuffer.allocate(Math.max(n, 2 * recbuf.capacity()));
				load_data_record(in, tablename, tran, recbuf, n);
				if (nrecs % 100 == 99) {
					tran.ck_complete();
					tran = db.readwriteTran();
				}
			}
		} finally {
			tran.ck_complete();
		}
		return nrecs;
	}

	private static void load_data_record(ReadableByteChannel in, String tablename,
			Transaction tran, ByteBuffer recbuf, int n)	throws IOException {
		fullRead(in, recbuf, n);
		Record rec = new Record(recbuf);
		if (tablename.equals("views"))
			Data.add_any_record(tran, tablename, rec);
		else
			Data.addRecord(tran, tablename, rec);
	}

	public static void main(String[] args) {
		long t = System.currentTimeMillis();
		DbTools.loadDatabasePrint(DatabasePackage.dbpkg, "suneido2.db", "database.su");
		System.out.println((System.currentTimeMillis() - t) + " ms");
	}

}
