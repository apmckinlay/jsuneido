package suneido.database;

import static suneido.util.Util.stringToBuffer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;

import suneido.SuException;

class DbDump {

	static void dumpDatabasePrint(String db_filename, String output_filename) {
		int n = dumpDatabase(db_filename, output_filename);
		System.out.println("dumped " + n + " tables from " + db_filename
				+ " to " + output_filename);
	}

	static int dumpDatabase(String db_filename, String output_filename) {
		Database db = new Database(db_filename, Mode.READ_ONLY);
		try {
			return dumpDatabase(db, output_filename);
		} finally {
			db.close();
		}
	}

	static int dumpDatabase(suneido.Database db, String output_filename) {
		try {
			return dumpDatabaseImp(db, output_filename);
		} catch (Throwable e) {
			throw new SuException("dump " + output_filename + " failed", e);
		}
	}

	private static int dumpDatabaseImp(suneido.Database db, String filename) throws Throwable {
		FileChannel fout = new FileOutputStream(filename).getChannel();
		try {
			Transaction t = (Transaction) db.readonlyTran();
			try {
				writeFileHeader(fout);
				BtreeIndex bti = t.getBtreeIndex(Database.TN.TABLES, "tablename");
				BtreeIndex.Iter iter = bti.iter().next();
				int n = 0;
				for (; !iter.eof(); iter.next()) {
					Record r = t.input(iter.keyadr());
					String tablename = r.getString(Table.TABLE);
					if (Schema.isSystemTable(tablename))
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

	static void dumpTablePrint(String db_filename, String tablename) {
		Database db = new Database(db_filename, Mode.READ_ONLY);
		try {
			int n = dumpTable(db, tablename);
			System.out.println("dumped " + n + " records from " + tablename);
		} finally {
			db.close();
		}
	}

	static int dumpTable(suneido.Database db, String tablename) {
		try {
			return dumpTableImp(db, tablename);
		} catch (Throwable e) {
			throw new SuException("dump " + tablename + " failed", e);
		}
	}

	private static int dumpTableImp(suneido.Database db, String tablename) throws Throwable {
		FileChannel fout = new FileOutputStream(tablename + ".su").getChannel();
		try {
			Transaction t = (Transaction) db.readonlyTran();
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

	private static int dump1(FileChannel fout, Transaction t, String tablename,
			boolean outputName) throws IOException {
		writeTableHeader(fout, t, tablename, outputName);
		return writeTableData(fout, t, tablename);
	}

	private static void writeTableHeader(FileChannel fout, Transaction t,
			String tablename, boolean outputName) throws IOException {
		String schema = t.ck_getTable(tablename).schema();
		String header = "====== ";
		if (outputName)
			header += tablename + " ";
		header += schema + "\n";
		write(fout, header);
	}

	private static int writeTableData(FileChannel fout, Transaction t,
			String tablename) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		Table table = t.getTable(tablename);
		List<String> fields = table.getFields();
		boolean squeeze = needToSqueeze(fields);
		Index index = table.firstIndex();
		BtreeIndex bti = t.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter().next();
		int n = 0;
		for (; !iter.eof(); iter.next()) {
			Record r = t.input(iter.keyadr());
			if (squeeze)
				r = squeezeRecord(r, fields);
			writeInt(fout, buf, r.bufSize());
			fout.write(r.getBuffer());
			++n;
		}
		writeInt(fout, buf, 0);
		return n;
	}

	private static final String DELETED = "-";

	static boolean needToSqueeze(List<String> fields) {
		return fields.indexOf(DELETED) != -1;
	}

	static Record squeezeRecord(Record rec, List<String> fields) {
		Record newrec = new Record(rec.packSize());
		int i = 0;
		for (String f : fields) {
			if (!f.equals("-"))
				newrec.add(rec.getbuf(i));
			++i;
		}
		return newrec.dup();
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
		dumpDatabasePrint("suneido.db", "database2.su");
//		dumpTablePrint("test");
	}

}
