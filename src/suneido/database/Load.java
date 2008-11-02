package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Database.theDB;
import static suneido.database.Mode.CREATE;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import suneido.SuException;

public class Load {
	private InputStream fin;
	byte[] recbuf = new byte[100];

	void load(String table) {
		// extern bool thedb_create;
		// thedb_create = true;

		try {
			if (!table.equals("")) { // load a single table
				fin = new BufferedInputStream(
						new FileInputStream(
						table + ".su"));

				String s = getline();
				if (!s.startsWith("Suneido dump"))
					throw new SuException("invalid file");

				String schema = getline();
				verify(schema.startsWith("====== "));
				schema = "create " + table + schema.substring(6);

				try {
					theDB.setLoading(true);
					load1(schema);
				} finally {
					theDB.setLoading(false);
				}
			}
		} catch (IOException e) {
			throw new SuException("Load: " + e);
		}
	}

	int load1(String schema) throws IOException {
System.out.println(schema);
		int n = schema.indexOf(' ', 7);
		String table = schema.substring(7, n);

		if (table != "views") {
		// if (theDB.isTable(table))
		// theDB.removeTable(table);
		// verify(database_admin(schema));
		}
		return load_data(table);
	}

	int load_data(String table) throws IOException {
System.out.println("load_data(" + table + ")");
		int nrecs = 0;
		Transaction tran = theDB.readwriteTran();
		try {
			for (;; ++nrecs) {
				byte[] buf = new byte[4];
				fin.read(buf);
				int n = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
						.getInt();
//System.out.println(n);
				if (n == 0)
					break;
				load_data_record(table, tran, n);
				if (nrecs % 100 == 99) {
					verify(tran.complete() == null);
					tran = theDB.readwriteTran();
				}
//break;
			}
		} finally {
			verify(tran.complete() == null);
		}
		return nrecs;
	}

	void load_data_record(String table, Transaction tran, int n)
			throws IOException {
		if (n > recbuf.length)
			recbuf = new byte[Math.max(n, 2 * recbuf.length)];
		fin.read(recbuf, 0, n);
		Record rec = new Record(ByteBuffer.wrap(recbuf, 0, n).order(
				ByteOrder.LITTLE_ENDIAN));
//System.out.println(rec);
System.out.println(rec.get(2));
		// try {
		// if (table == "views")
		// theDB.add_any_record(tran, table, rec);
		// else
		// theDB.add_record(tran, table, rec);
		// } catch (const Except& x) {
		// errlog("load: ignoring: ", table.str(), x.exception);
		// alert("ignoring: " << table << ": " << x.exception);
		// }
	}

	private String getline() throws IOException {
		String line = "";
		char c;
		while ('\n' != (c = (char) fin.read()))
			line += c;
		return line;
	}

	public static void main(String[] args) throws IOException {
		new File("suneido.db").delete();
		Mmfile mmf = new Mmfile("suneido.db", Mode.CREATE);
		Database.theDB = new Database(mmf, CREATE);

		new Load().load("stdlib");
	}

}
