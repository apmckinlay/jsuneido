package suneido.database;

import static suneido.SuException.verifyEquals;

import java.io.File;
import java.util.List;

import suneido.SuException;
import suneido.database.DbCheck.Status;
import suneido.database.query.Request;
import suneido.util.ByteBuf;
import suneido.util.FileUtils;

class DbCompact {
	private final String dbfilename;
	private final String tempfilename;
	private Database oldDB;
	private Database newDB;
	private Transaction rt;

	static void compactPrint(String dbfilename)
			throws InterruptedException {
		File tempfile = FileUtils.tempfile();
		compact2(dbfilename, tempfile.getPath());
		FileUtils.renameWithBackup(tempfile, dbfilename);
	}

	private static void compact2(String dbfilename, String tempfilename) {
		Status status = DbCheck.checkPrint(dbfilename);
		if (status != Status.OK)
			throw new SuException("Compact FAILED " + dbfilename + " " + status);
		System.out.println("Compacting " + dbfilename);
		int n = new DbCompact(dbfilename, tempfilename).compact();
		System.out.println(dbfilename + " compacted " + n + " tables");
	}

	static int compact(String dbfilename, String tempfilename) {
		Status status = DbCheck.check(dbfilename);
		if (status != Status.OK)
			throw new SuException("Compact FAILED " + dbfilename + " " + status);
		return new DbCompact(dbfilename, tempfilename).compact();
	}

	private DbCompact(String dbfilename, String tempfilename) {
		this.dbfilename = dbfilename;
		this.tempfilename = tempfilename;
	}

	private int compact() {
		File tempfile = new File(tempfilename);
		oldDB = new Database(dbfilename, Mode.READ_ONLY);
		newDB = new Database(tempfile, Mode.CREATE);

		int n = copy();

		oldDB.close();
		newDB.close();
		return n;
	}

	private int copy() {
		rt = oldDB.readonlyTran();
		newDB.setLoading(true);
		copySchema();
		return copyData() + 1; // + 1 for views
	}

	private void copySchema() {
		copyTable("views");
		BtreeIndex bti = rt.getBtreeIndex(Database.TN.TABLES, "tablename");
		BtreeIndex.Iter iter = bti.iter();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			String tablename = r.getString(Table.TABLE);
			if (!Schema.isSystemTable(tablename))
				createTable(tablename);
		}
	}

	private void createTable(String tablename) {
		Request.execute(newDB, "create " + tablename
				+ oldDB.getTable(tablename).schema());
		verifyEquals(oldDB.getTable(tablename).schema(), newDB.getTable(tablename).schema());
	}

	private int copyData() {
		BtreeIndex bti = rt.getBtreeIndex(Database.TN.TABLES, "tablename");
		BtreeIndex.Iter iter = bti.iter();
		int n = 0;
		for (iter.next(); ! iter.eof(); iter.next()) {
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
		Index index = table.firstIndex();
		BtreeIndex bti = rt.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter();
		int i = 0;
		long first = 0;
		long last = 0;
		Transaction wt = newDB.readwriteTran();
		int tblnum = wt.ck_getTable(tablename).num;
		for (iter.next(); !iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			if (squeeze)
				r = DbDump.squeezeRecord(r, fields);
			last = Data.outputRecordForCompact(wt, tblnum, r);
			if (first == 0)
				first = last;
			if (++i % 100 == 0) {
				wt.ck_complete();
				wt = newDB.readwriteTran();
			}
		}
		if (first != 0)
			createIndexes(wt, tblnum, first - 4, last - 4);
		wt.ck_complete();
	}

	private void createIndexes(Transaction wt, int tblnum, long first, long last) {
		Table table = wt.ck_getTable(tblnum);
		Mmfile mmf = (Mmfile) wt.db.dest;
		for (Index index : table.indexes) {
			Mmfile.Iter iter = mmf.iterator(first);
			do {
				if (iter.type() != Mmfile.DATA)
					continue;
				ByteBuf buf = iter.current();
				Record rec = new Record(iter.offset() + 4, buf.slice(4));
				newDB.addIndexEntriesForCompact(table, index, rec);
				if (iter.offset() >= last)
					break;
			} while (iter.next());
		}
	}

	public static void main(String[] args) throws InterruptedException {
		compactPrint("suneido.db");
	}

}
