/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import suneido.DbTools;
import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;

public class DbDump {

	static int dumpDatabase(Database db, WritableByteChannel out) {
		ReadTransaction t = db.readonlyTran();
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
		ReadTransaction t = db.readonlyTran();
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
		write(out, "Suneido dump 1.0\n");
	}

	private static int dump1(WritableByteChannel out, ReadTransaction t, String tablename,
			boolean outputName) throws IOException {
		writeTableHeader(out, t, tablename, outputName);
		return writeTableData(out, t, tablename);
	}

	private static void writeTableHeader(WritableByteChannel out, ReadTransaction t,
			String tablename, boolean outputName) throws IOException {
		String schema = t.ck_getTable(tablename).schema(t);
		String header = "====== ";
		if (outputName)
			header += tablename + " ";
		header += schema + "\n";
		write(out, header);
	}

	private static int writeTableData(WritableByteChannel out, ReadTransaction t,
			String tablename) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		Table table = t.getTable(tablename);
		List<String> fields = table.getFields();
		IndexIter iter = t.iter(table.num, null);
		int n = 0;
		for (iter.next(); !iter.eof(); iter.next()) {
			Record r = t.input(iter.keyadr());
			r = convert(r, fields);
			writeInt(out, buf, r.bufSize());
			out.write(r.getBuffer());
			++n;
		}
		writeInt(out, buf, 0);
		return n;
	}

	private static final String DELETED = "-";

	/** convert record to old format */
	static Record convert(Record rec, List<String> fields) {
		((suneido.immudb.Record) rec).check();
		RecordBuilder rb = new suneido.database.Record(rec.bufSize() + 2);
		int i = 0;
		for (String f : fields) {
			if (! f.equals(DELETED))
				rb.add(rec.getRaw(i));
			++i;
		}
		return rb.build();
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

	public static void main(String[] args) {
		DbTools.dumpDatabasePrint(DatabasePackage.dbpkg, "immudb.db", "immudb.su");
//		dumpTablePrint("test");
	}

}
