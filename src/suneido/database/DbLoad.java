package suneido.database;

import static suneido.SuException.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import suneido.SuException;
import suneido.database.query.Request;
import suneido.util.ByteBuf;
import suneido.util.FileUtils;

class DbLoad {

	static void loadDatabasePrint(String filename, String dbfilename)
			throws InterruptedException {
		File tempfile = FileUtils.tempfile();
		if (! DbTools.runWithNewJvm("-load:" + tempfile))
			throw new SuException("failed to load: " + filename);
		FileUtils.renameWithBackup(tempfile, dbfilename);
		System.out.println("loaded " + filename	+ " into new " + dbfilename);
	}

	static int load2(String filename, String dbfilename) {
		try {
			return loadDatabase(filename, dbfilename);
		} catch (Throwable e) {
			throw new SuException("load " + filename + " failed", e);
		}
	}

	static int loadDatabase(String filename, String dbfilename)
			throws Throwable {
		int n = 0;
		File dbfile = new File(dbfilename);
		Database db = new Database(dbfile, Mode.CREATE);
		try {
			// NOTE: using BufferedInputStream here causes problems
			InputStream fin = new FileInputStream(filename);
			try {
				verifyFileHeader(fin);
				String schema;
				try {
					db.setLoading(true);
					while (null != (schema = readTableHeader(fin))) {
						schema = "create" + schema.substring(6);
						load1(db, fin, schema);
						++n;
					}
				} finally {
					db.setLoading(false);
				}
			} finally {
				fin.close();
			}
		} finally {
			db.close();
		}
		return n;
	}

	static void loadTablePrint(String tablename) {
		int n = loadTable(tablename);
		System.out.println("loaded " + n + " records into suneido.db");
	}

	static int loadTable(String tablename) {
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
		Database db = new Database(dbfile, mode);
		try {
			InputStream fin = new FileInputStream(tablename + ".su");
			try {
				verifyFileHeader(fin);
				String schema = readTableHeader(fin);
				if (schema == null)
					throw new SuException("not a valid dump file");
				schema = "create " + tablename + schema.substring(6);
				try {
					db.setLoading(true);
					return load1(db, fin, schema);
				} finally {
					db.setLoading(false);
				}
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

	private static int load1(Database db, InputStream fin, String schema) throws IOException {
		int n = schema.indexOf(' ', 7);
		String table = schema.substring(7, n);

		if (!"views".equals(table)) {
			Schema.removeTable(db, table);
			Request.execute(db, schema);
		}
		return load_data(db, fin, table);
	}

	private static int load_data(Database db, InputStream fin, String tablename) throws IOException {
		int nrecs = 0;
		byte[] buf = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
		byte[] recbuf = new byte[4096];
		Transaction tran = db.readwriteTran();
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
					tran = db.readwriteTran();
				}
			}
		} finally {
			tran.ck_complete();
		}
		return nrecs;
	}

	private static void load_data_record(InputStream fin, String tablename,
			Transaction tran, byte[] recbuf, int n)	throws IOException {
		fullRead(fin, recbuf, n);
		Record rec = new Record(ByteBuf.wrap(recbuf, 0, n));
		if (tablename.equals("views"))
			Data.add_any_record(tran, tablename, rec);
		else
			Data.addRecord(tran, tablename, rec);
	}

	private static void fullRead(InputStream fin, byte[] recbuf, int n)
			throws IOException {
		int nread = 0;
		do {
			int nr = fin.read(recbuf, 0, n - nread);
			if (nr == -1)
				break;
			nread += nr;
		} while (nread < n);
		if (nread != n)
			throw new SuException("premature eof reading record of length " + n);
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

	public static void main(String[] args) throws InterruptedException {
		loadDatabasePrint("database.su", "dbload.db");

//		int n = Load.loadTable("stdlib");
//		System.out.println("loaded " + n + " records into stdlib");
//		n = Load.loadTable("Accountinglib");
//		System.out.println("loaded " + n + " records into Accountinglib");
	}

}
