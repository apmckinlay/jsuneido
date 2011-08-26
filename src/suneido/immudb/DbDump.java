/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.DatabasePackage.DB_FILENAME;
import static suneido.util.Util.stringToBuffer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;

import suneido.SuException;
import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;

public class DbDump {

	static void dumpDatabasePrint(String dbFilename, String outputFilename) {
		int n = dumpDatabase(dbFilename, outputFilename);
		System.out.println("dumped " + n + " tables from " + dbFilename
				+ " to " + outputFilename);
	}

	static int dumpDatabase(String dbFilename, String outputFilename) {
		Database db = Database.open(dbFilename, "r");
		try {
			return dumpDatabase(db, outputFilename);
		} finally {
			db.close();
		}
	}

	static int dumpDatabase(Database db, String outputFilename) {
		try {
			return dumpDatabaseImp(db, outputFilename);
		} catch (Throwable e) {
			throw new SuException("dump " + outputFilename + " failed", e);
		}
	}

	private static int dumpDatabaseImp(Database db, String filename) throws Throwable {
		FileChannel fout = new FileOutputStream(filename).getChannel();
		try {
			ReadTransaction t = db.readonlyTran();
			try {
				writeFileHeader(fout);
				IndexIter iter = t.iter(Bootstrap.TN.TABLES, "tablename");
				int n = 0;
				for (iter.next(); ! iter.eof(); iter.next()) {
					Record r = t.input(iter.keyadr());
					String tablename = r.getString(Table.TABLE);
					if (Database.isSystemTable(tablename))
						continue;
					dump1(fout, t, tablename, true);
					++n;
				}
				dump1(fout, t, "views", true);
				return ++n;
			} finally {
				t.complete();
			}
		} finally {
			fout.close();
		}
	}

	static void dumpTablePrint(String dbFilename, String tablename) {
		Database db = Database.open(dbFilename, "r");
		try {
			int n = dumpTable(db, tablename);
			System.out.println("dumped " + n + " records from " + tablename);
		} finally {
			db.close();
		}
	}

	static int dumpTable(Database db, String tablename) {
		try {
			return dumpTableImp(db, tablename);
		} catch (Throwable e) {
			throw new SuException("dump " + tablename + " failed", e);
		}
	}

	private static int dumpTableImp(Database db, String tablename) throws Throwable {
		FileChannel fout = new FileOutputStream(tablename + ".su").getChannel();
		try {
			ReadTransaction t = db.readonlyTran();
			try {
				writeFileHeader(fout);
				return dump1(fout, t, tablename, false);
			} finally {
				t.complete();
			}
		} finally {
			fout.close();
		}
	}

	private static void writeFileHeader(FileChannel fout) throws IOException {
		write(fout, "Suneido dump 1.0\n");
	}

	private static int dump1(FileChannel fout, ReadTransaction t, String tablename,
			boolean outputName) throws IOException {
		writeTableHeader(fout, t, tablename, outputName);
		return writeTableData(fout, t, tablename);
	}

	private static void writeTableHeader(FileChannel fout, ReadTransaction t,
			String tablename, boolean outputName) throws IOException {
		String schema = t.ck_getTable(tablename).schema(t);
		String header = "====== ";
		if (outputName)
			header += tablename + " ";
		header += schema + "\n";
		write(fout, header);
	}

	private static int writeTableData(FileChannel fout, ReadTransaction t,
			String tablename) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		Table table = t.getTable(tablename);
		List<String> fields = table.getFields();
		IndexIter iter = t.iter(table.num, null);
		int n = 0;
		for (iter.next(); !iter.eof(); iter.next()) {
			Record r = t.input(iter.keyadr());
			r = convert(r, fields);
			writeInt(fout, buf, r.bufSize());
			fout.write(r.getBuffer());
			++n;
		}
		writeInt(fout, buf, 0);
		return n;
	}

	private static final String DELETED = "-";

	/** convert record to old format */
	private static Record convert(Record rec, List<String> fields) {
		((suneido.immudb.Record) rec).check();
		RecordBuilder rb = new suneido.database.Record(rec.bufSize() + 2);
		int i = 0;
		for (String f : fields) {
			if (! f.equals("-"))
				rb.add(rec.getRaw(i));
			++i;
		}
		return rb.build();
	}

	private static void writeInt(FileChannel fout, ByteBuffer buf, int n)
			throws IOException {
		buf.putInt(0, n);
		buf.rewind();
		fout.write(buf);
	}

	private static void write(FileChannel fout, String s) throws IOException {
		fout.write(stringToBuffer(s));
	}

	public static void main(String[] args) {
		dumpDatabasePrint(DB_FILENAME, "immudb.su");
//		dumpTablePrint("test");
	}

}
