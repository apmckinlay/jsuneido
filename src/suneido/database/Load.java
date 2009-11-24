package suneido.database;

import static suneido.Suneido.verify;
import static suneido.database.Database.theDB;
import static suneido.database.Mode.CREATE;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import suneido.SuException;
import suneido.database.query.Request;
import suneido.util.ByteBuf;

public class Load {
	private InputStream fin;
	byte[] recbuf = new byte[100];

	public void load(String table) {
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

		if (!"views".equals(table)) {
			Schema.removeTable(theDB, table);
			Request.execute(schema);
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
				verify(fin.read(buf) == buf.length);
				int n = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
						.getInt();
				if (n == 0)
					break;
				load_data_record(table, tran, n);
				if (nrecs % 100 == 99) {
					verify(tran.complete() == null);
					tran = theDB.readwriteTran();
				}
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
		verify(fin.read(recbuf, 0, n) == n);
		Record rec = new Record(ByteBuf.wrap(recbuf, 0, n));
//System.out.println(rec.get(0));

		try {
			if (table.equals("views"))
				Data.add_any_record(tran, table, rec);
			else
				Data.addRecord(tran, table, rec);
		} catch (Throwable e) {
			System.out.println("load: ignoring: " + table + e);
		}
	}

//	private static void printbuf(String name, ByteBuffer b) {
//		System.out.print(name);
//		for (int i = 0; i < b.limit() && i < 20; ++i)
//			System.out.print(" " + (b.get(i) & 0xff));
//		System.out.println("");
//	}

	private String getline() throws IOException {
		StringBuilder sb = new StringBuilder();
		char c;
		while ('\n' != (c = (char) fin.read()))
			sb.append(c);
		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		new File("suneido.db").delete();
		Mmfile mmf = new Mmfile("suneido.db", Mode.CREATE);
		Database.theDB = new Database(mmf, CREATE);

		new Load().load("stdlib");
		new Load().load("Accountinglib");

		Database.theDB.close();
		Database.theDB = null;
	}

}
