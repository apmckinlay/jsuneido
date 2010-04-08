package suneido.database.tools;

import static suneido.SuException.verify;
import static suneido.database.Database.theDB;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import suneido.SuException;
import suneido.database.*;
import suneido.database.query.Request;
import suneido.util.ByteBuf;

public class DbLoad {

	public static void loadPrint(String filename) {
		int n = DbLoad.loadDatabase(filename);
		System.out.println("loaded " + n + " tables from " + filename
				+ " into new suneido.db");
	}

	public static int loadDatabase(String filename) {
		try {
			return loadDatabaseImp(filename);
		} catch (Throwable e) {
			throw new SuException("load " + filename + " failed", e);
		}
	}

	public static int loadDatabaseImp(String filename)
			throws Throwable {
		int n = 0;
		File tmpfile = File.createTempFile("sudb", null, new File("."));
		try {
			Database.theDB = new Database(tmpfile, Mode.CREATE);
			InputStream fin = new BufferedInputStream(
					new FileInputStream(filename));
			try {
				verifyFileHeader(fin);
				String schema;
				try {
					theDB.setLoading(true);
					while (null != (schema = readTableHeader(fin))) {
						schema = "create" + schema.substring(6);
						load1(fin, schema);
						++n;
					}
				} finally {
					theDB.setLoading(false);
				}
			} finally {
				fin.close();
			}
		} catch (Throwable e) {
			tmpfile.delete();
			throw e;
		} finally {
			Database.theDB.close();
		}
		File bak = new File("suneido.db.bak");
		if (bak.exists())
			verify(bak.delete());
		File sdb = new File("suneido.db");
		if (sdb.exists())
			verify(sdb.renameTo(bak));
		verify(tmpfile.renameTo(sdb));
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

	private static int loadTableImp(String tablename) throws Throwable {
		if (tablename.endsWith(".su"))
			tablename = tablename.substring(0, tablename.length() - 3);
		File dbfile = new File("suneido.db");
		Mode mode = dbfile.exists() ? Mode.OPEN : Mode.CREATE;
		Database.theDB = new Database(dbfile, mode);
		try {
			InputStream fin = new BufferedInputStream(
					new FileInputStream(tablename + ".su"));
			try {
				verifyFileHeader(fin);
				String schema = readTableHeader(fin);
				if (schema == null)
					throw new SuException("not a valid dump file");
				schema = "create " + tablename + schema.substring(6);
				try {
					theDB.setLoading(true);
					return load1(fin, schema);
				} finally {
					theDB.setLoading(false);
				}
			} finally {
				fin.close();
			}
		} finally {
			Database.theDB.close();
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
		int c;
		while ('\n' != (c = fin.read())) {
			if (c == -1)
				return null;
			sb.append((char) c);
		}
		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		loadPrint("database.su");

//		int n = Load.loadTable("stdlib");
//		System.out.println("loaded " + n + " records into stdlib");
//		n = Load.loadTable("Accountinglib");
//		System.out.println("loaded " + n + " records into Accountinglib");
	}

}
