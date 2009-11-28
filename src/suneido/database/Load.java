package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Database.theDB;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import suneido.SuException;
import suneido.database.query.Request;
import suneido.util.ByteBuf;

public class Load {

	public static int loadTable(String tablename) {
		try {
			return loadTableImp(tablename);
		} catch (Throwable e) {
			throw new SuException("load " + tablename + " failed", e);
		}
	}

	private static int loadTableImp(String tablename) throws Throwable {
		InputStream fin = new BufferedInputStream(
				new FileInputStream(tablename + ".su"));
		try {
			String schema = readHeader(fin, tablename);
			try {
				theDB.setLoading(true);
				return load1(fin, schema);
			} finally {
				theDB.setLoading(false);
			}
		} finally {
			fin.close();
		}
	}

	private static String readHeader(InputStream fin, String tablename) throws IOException {
		String s = getline(fin);
		if (!s.startsWith("Suneido dump"))
			throw new SuException("invalid file");

		String schema = getline(fin);
		verify(schema.startsWith("====== "));
		schema = "create " + tablename + schema.substring(6);
		return schema;
	}

	private static int load1(InputStream fin, String schema) throws IOException {
		int n = schema.indexOf(' ', 7);
		String table = schema.substring(7, n);

		if (!"views".equals(table)) {
			Schema.removeTable(theDB, table);
			Request.execute(schema);
		}
		return load_data(fin, table);
	}

	private static int load_data(InputStream fin, String tablename) throws IOException {
		int nrecs = 0;
		byte[] buf = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
		byte[] recbuf = new byte[4096];
		Transaction tran = theDB.readwriteTran();
		try {
			for (;; ++nrecs) {
				verify(fin.read(buf) == buf.length);
				int n = bb.getInt(0);
				if (n == 0)
					break;
				if (n > recbuf.length)
					recbuf = new byte[Math.max(n, 2 * recbuf.length)];
				load_data_record(fin, tablename, tran, recbuf, n);
				if (nrecs % 100 == 99) {
					verify(tran.complete() == null);
					tran = theDB.readwriteTran();
				}
			}
		} finally {
			tran.ck_complete();
		}
		return nrecs;
	}

	private static void load_data_record(InputStream fin, String tablename,
			Transaction tran, byte[] recbuf, int n)	throws IOException {
		verify(fin.read(recbuf, 0, n) == n);
		Record rec = new Record(ByteBuf.wrap(recbuf, 0, n));
		try {
			if (tablename.equals("views"))
				Data.add_any_record(tran, tablename, rec);
			else
				Data.addRecord(tran, tablename, rec);
		} catch (Throwable e) {
			System.out.println("load failed for " + tablename + e);
		}
	}

	private static String getline(InputStream fin) throws IOException {
		StringBuilder sb = new StringBuilder();
		char c;
		while ('\n' != (c = (char) fin.read()))
			sb.append(c);
		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		new File("suneido.db").delete();
		Mmfile mmf = new Mmfile("suneido.db", Mode.CREATE);
		Database.theDB = new Database(mmf, Mode.CREATE);

		int n = Load.loadTable("stdlib");
		System.out.println("loaded " + n + " records into stdlib");
		n = Load.loadTable("Accountinglib");
		System.out.println("loaded " + n + " records into Accountinglib");

		Database.theDB.close();
		Database.theDB = null;
	}

}
