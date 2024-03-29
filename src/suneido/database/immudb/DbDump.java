/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.ByteBuffers.stringToBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.List;

class DbDump {
	static final String version = "Suneido dump 3";
	static final String versionPrev = "Suneido dump 2";
	static final String versionBase = "Suneido dump";

	static int dumpDatabase(Database db, WritableByteChannel out) {
		ReadTransaction t = db.readTransaction();
		try {
			writeFileHeader(out);
			IndexIter iter = t.iter(Bootstrap.TN.TABLES, "tablename");
			int n = 0;
			for (iter.next(); ! iter.eof(); iter.next()) {
				Record r = t.input(iter.keyadr());
				String tablename = r.getString(Table.TABLE);
				if (Database.isSystemTable(tablename))
					continue;
				dump1(out, t, tablename, true);
				++n;
			}
			dump1(out, t, "views", true);
			return ++n;
		} catch (Exception e) {
			throw new RuntimeException("dump failed", e);
		} finally {
			t.complete();
		}
	}

	static int dumpTable(Database db, String tablename, WritableByteChannel out) {
		ReadTransaction t = db.readTransaction();
		try {
			writeFileHeader(out);
			return dump1(out, t, tablename, false);
		} catch (Exception e) {
			throw new RuntimeException("dump failed", e);
		} finally {
			t.complete();
		}
	}

	private static void writeFileHeader(WritableByteChannel out) throws IOException {
		write(out, version + "\n");
	}

	private static int dump1(WritableByteChannel out, ReadTransaction t, String tablename,
			boolean outputName) throws IOException {
		writeTableHeader(out, t, tablename, outputName);
		return writeTableData(out, t, tablename);
	}

	private static void writeTableHeader(WritableByteChannel out, ReadTransaction t,
			String tablename, boolean outputName) throws IOException {
		String schema = t.ck_getTable(tablename).schema();
		StringBuilder header = new StringBuilder("====== ");
		if (outputName)
			header.append(tablename).append(" ");
		header.append(schema).append("\n");
		write(out, header.toString());
	}

	private static int writeTableData(WritableByteChannel out, ReadTransaction t,
			String tablename) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		Table table = t.getTable(tablename);
		List<String> fields = table.getFields();
		boolean squeeze = needToSqueeze(t, table.num, fields);
		IndexIter iter = t.iter(table.num, null);
		int n = 0;
		for (iter.next(); !iter.eof(); iter.next()) {
			Record r = t.input(iter.keyadr());
			if (squeeze)
				r = squeezeRecord(r, fields).bufRec();
			writeInt(out, buf, r.bufSize());
			out.write(r.getBuffer());
			++n;
		}
		writeInt(out, buf, 0);
		return n;
	}

	private static final String DELETED = "-";

	static boolean needToSqueeze(ReadTransaction t, int tblnum, List<String> fields) {
		TableInfo ti = t.getTableInfo(tblnum);
		return ti.nextfield > fields.size() || fields.indexOf(DELETED) != -1;
	}

	static RecordBuilder squeezeRecord(Record rec, List<String> fields) {
		RecordBuilder rb = new RecordBuilder();
		int i = 0;
		for (String f : fields) {
			if (! f.equals(DELETED))
				rb.add(rec.getRaw(i));
			++i;
		}
		return rb;
	}

	private static void writeInt(WritableByteChannel out, ByteBuffer buf, int n)
			throws IOException {
		buf.putInt(0, n);
		buf.rewind();
		out.write(buf);
	}

	private static void write(WritableByteChannel out, String s) throws IOException {
		out.write(stringToBuffer(s));
	}

//	public static void main(String[] args) {
////		DbTools.dumpPrintExit(DatabasePackage.dbpkg, "immu.compact", "immu.su");
//		DbTools.dumpTablePrint(DatabasePackage.dbpkg, "suneido.db", "tmp");
//	}

}
