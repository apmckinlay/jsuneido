/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.Suneido.dbpkg;
import static suneido.util.FileUtils.fullRead;
import static suneido.util.FileUtils.getline;
import static suneido.util.FileUtils.readInt;
import static suneido.util.Verify.verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

import suneido.DbTools;
import suneido.Suneido;
import suneido.database.query.Request;

import com.google.common.base.Stopwatch;

class DbLoad {

	static int loadDatabase(Database db, ReadableByteChannel in) {
		try {
			verifyFileHeader(in);
			String schema;
			int n = 0;
			while (null != (schema = readTableHeader(in))) {
				schema = "create" + schema.substring(6);
				load1(db, in, schema);
				++n;
			}
			return n;
		} catch (Exception e) {
			throw new RuntimeException("load failed", e);
		}
	}

	static int loadTable(Database db, String tablename, ReadableByteChannel in) {
		try {
			verifyFileHeader(in);
			String schema = readTableHeader(in);
			if (schema == null)
				throw new RuntimeException("not a valid dump file");
			schema = "create " + tablename + schema.substring(6);
			db.dropTable(tablename);
			return load1(db, in, schema);
		} catch (Exception e) {
			throw new RuntimeException("load failed", e);
		}
	}

	private static void verifyFileHeader(ReadableByteChannel in)
			throws IOException {
		String s = getline(in);
		if (s == null || ! s.startsWith("Suneido dump"))
			throw new RuntimeException("not a valid dump file");
	}

	private static String readTableHeader(ReadableByteChannel in)
			throws IOException {
		String schema = getline(in);
		if (schema == null)
			return null;
		verify(schema.startsWith("====== "));
		return schema;
	}

	private static int load1(Database db, ReadableByteChannel in, String schema)
			throws IOException {
		int i = schema.indexOf(' ', 7);
		String table = schema.substring(7, i);
		if (! "views".equals(table))
			Request.execute(db, schema);
		return load_data(db, in, table);
	}

	private static int load_data(Database db, ReadableByteChannel in, String tablename)
			throws IOException {
		print(tablename);
		int nrecs = 0;
		ByteBuffer intbuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer recbuf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
		BulkTransaction t = db.bulkTransaction();
		try {
			Table table = t.getTable(tablename);
			int first = 0;
			int last = 0;
			for (;; ++nrecs) {
				if (nrecs % 10000 == 0)
					print(".");
				int n = readInt(in, intbuf);
				if (n == 0)
					break;
				if (n > recbuf.capacity())
					recbuf = ByteBuffer.allocate(Math.max(n, 2 * recbuf.capacity()))
							.order(ByteOrder.LITTLE_ENDIAN);
				last = load_data_record(in, table.num, t, recbuf, n);
				if (first == 0)
					first = last;
			}
			print(nrecs + "\n");
			createIndexes(t, table, first, last);
			t.ck_complete();
		} finally {
			t.abortIfNotComplete();
		}
		return nrecs;
	}

	private static int load_data_record(ReadableByteChannel in, int tblnum,
			BulkTransaction t, ByteBuffer recbuf, int n)
			throws IOException {
		fullRead(in, recbuf, n);
		DataRecord rec = new DataRecord(recbuf);
		return t.loadRecord(tblnum, rec);
	}

	static void createIndexes(BulkTransaction t, Table table, int first, int last) {
		if (first == 0)
			return; // no data
		for (Index index : table.indexes) {
			print("\t" + index.columns(table.fields));
			createIndex(t, first, last, index);
		}
	}

	private static void createIndex(BulkTransaction t, int first, int last, Index index) {
		TranIndex btree = t.getIndex(index.tblnum, index.colNums);
		StoredRecordIterator iter = t.storedRecordIterator(first, last);
		int i = 0;
		while (iter.hasNext()) {
			if (i++ % 10000 == 0)
				print(".");
			int adr = iter.nextAdr();
			Record rec = iter.next();
			BtreeKey key = IndexedData.key(rec, index.colNums, adr);
			btree.add(key, false);
		}
		print("^");
		t.saveBtrees();
		print("\n");
	}

	private static void print(String s) {
		//System.out.print(s);
	}

	public static void main(String[] args) throws IOException  {
		Stopwatch sw = new Stopwatch().start();
		Database db = (Database) dbpkg.create(dbpkg.dbFilename());
		@SuppressWarnings("resource")
		ReadableByteChannel fin = new FileInputStream("database.su").getChannel();
		loadDatabase(db, fin);
		db.close();
		System.out.println(sw);
		DbTools.checkPrint(Suneido.dbpkg, dbpkg.dbFilename());
	}

}