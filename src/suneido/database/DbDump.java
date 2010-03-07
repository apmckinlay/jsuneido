package suneido.database;

import static suneido.database.Database.theDB;
import static suneido.util.Util.stringToBuffer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;

import suneido.SuException;

public class DbDump {

	public static void dumpDatabasePrint(String db_filename, String output_filename) {
		int n = dumpDatabase(db_filename, output_filename);
		System.out.println("dumped " + n + " tables from " + db_filename
				+ " to " + output_filename);
	}

	public static int dumpDatabase(String db_filename, String output_filename) {
		theDB = new Database(db_filename, Mode.READ_ONLY);
		try {
			return dumpDatabase(output_filename);
		} finally {
			theDB.close();
			theDB = null;
		}
	}

	public static int dumpDatabase(String output_filename) {
		try {
			return dumpDatabaseImp(output_filename);
		} catch (Throwable e) {
			throw new SuException("dump " + output_filename + " failed", e);
		}
	}

	private static int dumpDatabaseImp(String filename) throws Throwable {
		FileChannel fout = new FileOutputStream(filename).getChannel();
		try {
			Transaction t = theDB.readonlyTran();
			try {
				writeFileHeader(fout);
				BtreeIndex bti = t.getBtreeIndex(Database.TN.TABLES, "tablename");
				BtreeIndex.Iter iter = bti.iter(t).next();
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

	public static void dumpTablePrint(String tablename) {
		if (theDB == null)
			Database.open_theDB();
		int n = dumpTable(tablename);
		System.out.println("dumped " + n + " records from " + tablename);
	}

	public static int dumpTable(String tablename) {
		try {
			return dumpTableImp(tablename);
		} catch (Throwable e) {
			throw new SuException("dump " + tablename + " failed", e);
		}
	}

	private static int dumpTableImp(String tablename) throws Throwable {
		FileChannel fout = new FileOutputStream(tablename + ".su").getChannel();
		try {
			Transaction t = theDB.readonlyTran();
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
		Index index = table.indexes.first();
		BtreeIndex bti = t.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter(t).next();
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

	private final static String DELETED = "-";

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
