package suneido.database;

import static suneido.util.Verify.verifyEquals;

import java.util.List;

import suneido.DbTools;
import suneido.database.query.Request;
import suneido.util.ByteBuf;

class DbCompact {
	private final Database olddb;
	private final Database newdb;
	private Transaction rt;

	static int compact(Database olddb, Database newdb) {
		return new DbCompact(olddb, newdb).compact();
	}

	private DbCompact(Database olddb, Database newdb) {
		this.olddb = olddb;
		this.newdb = newdb;
	}

	private int compact() {
		rt = olddb.readTransaction();
		newdb.setLoading(true);
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
		Request.execute(newdb, "create " + tablename
				+ olddb.getTable(tablename).schema());
		verifyEquals(olddb.getTable(tablename).schema(), newdb.getTable(tablename).schema());
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
		Transaction wt = newdb.updateTransaction();
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
				wt = newdb.updateTransaction();
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
				newdb.addIndexEntriesForCompact(table, index, rec);
				if (iter.offset() >= last)
					break;
			} while (iter.next());
		}
	}

	public static void main(String[] args) {
		DbTools.compactPrintExit(DatabasePackage.dbpkg, "suneido.db");
	}

}
