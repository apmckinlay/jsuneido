package suneido.database.tools;

import static suneido.SuException.verifyEquals;

import java.io.File;
import java.util.List;

import suneido.SuException;
import suneido.database.*;
import suneido.database.query.Request;
import suneido.database.tools.DbCheck.Status;
import suneido.util.ByteBuf;
import suneido.util.FileUtils;

public class DbCompact {
	private final String dbfilename;
	private final String tempfilename;
	private Database oldDB;
	private Transaction rt;

	public static void compactPrint(String dbfilename)
			throws InterruptedException {
		File tempfile = FileUtils.tempfile();
		if (!DbTools.runWithNewJvm("-compact:" + tempfile))
			throw new SuException("compact failed: " + dbfilename);
		FileUtils.renameWithBackup(tempfile, dbfilename);
	}

	public static void compact2(String dbfilename, String tempfilename) {
		Status status = DbCheck.checkPrint(dbfilename);
		if (status != Status.OK)
			throw new SuException("Compact FAILED " + dbfilename + " " + status);
		System.out.println("Compacting " + dbfilename);
		int n = new DbCompact(dbfilename, tempfilename).compact();
		System.out.println(dbfilename + " compacted " + n + " tables");
	}

	public static int compact(String dbfilename, String tempfilename) {
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
		TheDb.set(new Database(tempfile, Mode.CREATE));

		int n = copy();

		oldDB.close();
		TheDb.db().close();
		return n;
	}

	private int copy() {
		rt = oldDB.readonlyTran();
		TheDb.db().loading = true;
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
		verifyEquals(oldDB.getTable(tablename).schema(), TheDb.db().getTable(tablename).schema());
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
		Transaction wt = TheDb.db().readwriteTran();
		int tblnum = wt.ck_getTable(tablename).num;
		for (; !iter.eof(); iter.next()) {
			Record r = rt.input(iter.keyadr());
			if (squeeze)
				r = DbDump.squeezeRecord(r, fields);
			last = Data.outputRecordForCompact(wt, tblnum, r);
			if (first == 0)
				first = last;
			if (++i % 100 == 0) {
				wt.ck_complete();
				wt = TheDb.db().readwriteTran();
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
				Record rec = new Record(buf.slice(4), iter.offset() + 4);
				TheDb.db().addIndexEntriesForCompact(table, index, rec);
				if (iter.offset() >= last)
					break;
			} while (iter.next());
		}
	}

	public static void main(String[] args) throws InterruptedException {
		compactPrint("suneido.db");
	}

}
