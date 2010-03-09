package suneido.database;

import static suneido.SuException.verifyEquals;
import static suneido.database.Database.theDB;

import java.io.File;
import java.io.IOException;
import java.util.List;

import suneido.SuException;
import suneido.database.DbCheck.Status;
import suneido.database.query.Request;
import suneido.util.ByteBuf;

public class DbCompact {
	private final String dbFilename;
	private Database oldDB;
	private Transaction rt;

	public static void compactPrint(String db_filename) {
		Status status = DbCheck.checkPrint(db_filename);
		if (status != Status.OK)
			throw new SuException("Compact FAILED " + db_filename + " " + status);
		System.out.println("Compacting " + db_filename);
		int n = new DbCompact(db_filename).compact();
		System.out.println(db_filename + " compacted " + n + " tables");
	}

	public static int compact(String db_filename) {
		Status status = DbCheck.check(db_filename);
		if (status != Status.OK)
			throw new SuException("Compact FAILED " + db_filename + " " + status);
		return new DbCompact(db_filename).compact();
	}

	private DbCompact(String dbFilename) {
		this.dbFilename = dbFilename;
	}

	private int compact() {
		File tmpfile = tmpfile();
		oldDB = new Database(dbFilename, Mode.READ_ONLY);
		theDB = new Database(tmpfile, Mode.CREATE);

		int n = copy();

		oldDB.close();
		theDB.close();
		theDB = null;

		File dbfile = new File(dbFilename);
		File bakfile = new File(dbFilename + ".bak");
		bakfile.delete();
		dbfile.renameTo(bakfile);
		tmpfile.renameTo(dbfile);

		return n;
	}

	private File tmpfile() {
		File tmpfile;
		try {
			tmpfile = File.createTempFile("sudb", null, new File("."));
		} catch (IOException e) {
			throw new SuException("Can't create temp file", e);
		}
		return tmpfile;
	}

	private int copy() {
		rt = oldDB.readonlyTran();
		theDB.loading = true;
		copySchema();
		return copyData() + 1; // + 1 for views
	}

	private void copySchema() {
		copyTable("views");
		BtreeIndex bti = rt.getBtreeIndex(Database.TN.TABLES, "tablename");
		BtreeIndex.Iter iter = bti.iter(rt).next();
		for (; !iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			String tablename = r.getString(Table.TABLE);
			if (!Schema.isSystemTable(tablename))
				createTable(tablename);
		}
	}

	private void createTable(String tablename) {
		Request.execute("create " + tablename
				+ oldDB.getTable(tablename).schema());
		verifyEquals(oldDB.getTable(tablename).schema(), theDB.getTable(tablename).schema());
	}

	private int copyData() {
		BtreeIndex bti = rt.getBtreeIndex(Database.TN.TABLES, "tablename");
		BtreeIndex.Iter iter = bti.iter(rt).next();
		int n = 0;
		for (; !iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			String tablename = r.getString(Table.TABLE);
			if (!Schema.isSystemTable(tablename)) {
				copyTable(tablename);
				++n;
			}
		}
		return n;
	}

	private void copyTable(String tablename) {
		Table table = rt.ck_getTable(tablename);
		List<String> fields = table.getFields();
		boolean squeeze = DbDump.needToSqueeze(fields);
		Index index = table.indexes.first();
		BtreeIndex bti = rt.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter(rt).next();
		int i = 0;
		long first = 0;
		long last = 0;
		Transaction wt = theDB.readwriteTran();
		for (; !iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			if (squeeze)
				r = DbDump.squeezeRecord(r, fields);
			last = Data.outputRecordForCompact(wt, table, r);
			if (first == 0)
				first = last;
			if (++i % 100 == 0) {
				wt.ck_complete();
				wt = theDB.readwriteTran();
			}
		}
		if (first != 0)
			createIndexes(wt, table, first - 4, last - 4);
		wt.ck_complete();
	}

	private void createIndexes(Transaction wt, Table table, long first, long last) {
		Mmfile mmf = (Mmfile) wt.db.dest;
		for (Index index : table.indexes) {
			Mmfile.MmfileIterator iter = mmf.iterator(first);
			while (iter.hasNext()) {
				ByteBuf buf = iter.next();
				if (iter.type() != Mmfile.DATA)
					continue;
				Record rec = new Record(buf.slice(4), iter.offset() + 4);
				theDB.addIndexEntriesForCompact(table, index, rec);
				if (iter.offset() >= last)
					break;
			}
		}
	}

	public static void main(String[] args) {
		compactPrint("suneido.db");
	}

}
