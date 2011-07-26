/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

class TestBaseBase {
	protected final DestMem dest = new DestMem();
	protected Database db = new Database(dest, Mode.CREATE);

	protected void makeTable() {
		makeTable(0);
	}

	protected void makeTable(String tablename) {
		makeTable(tablename, 0);
	}

	protected void makeTable(int nrecords) {
		makeTable("test", nrecords);
	}

	protected void makeTable(String tablename, int nrecords) {
		db.addTable(tablename);
		db.addColumn(tablename, "a");
		db.addColumn(tablename, "b");
		db.addIndex(tablename, "a", true);
		db.addIndex(tablename, "b,a", false);

		addRecords(tablename, 0, nrecords - 1);
	}

	protected void addRecords(String tablename, int from, int to) {
		while (from <= to) {
			Transaction t = db.readwriteTran();
			for (int i = 0; i < 1000 && from <= to; ++i, ++from)
				t.addRecord(tablename, record(from));
			t.ck_complete();
		}
	}

	protected List<Record> get() {
		return get("test");
	}

	protected List<Record> get(String tablename) {
		Transaction tran = db.readonlyTran();
		List<Record> recs = get(tablename, tran);
		tran.ck_complete();
		return recs;
	}

	protected List<Record> get(Transaction tran) {
		return get("test", tran);
	}

	protected List<Record> get(String tablename, Transaction tran) {
		List<Record> recs = new ArrayList<Record>();
		Table table = tran.getTable(tablename);
		Index index = table.indexes.first();
		BtreeIndex bti = tran.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter();
		for (iter.next(); ! iter.eof(); iter.next())
			recs.add(db.input(iter.keyoff()));
		return recs;
	}

	protected Record getFirst(String tablename, Transaction tran) {
		Table table = tran.getTable(tablename);
		Index index = table.indexes.first();
		BtreeIndex bti = tran.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter();
		iter.next();
		return iter.eof() ? null : db.input(iter.keyoff());
	}

	protected Record getLast(String tablename, Transaction tran) {
		Table table = tran.getTable(tablename);
		Index index = table.indexes.first();
		BtreeIndex bti = tran.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter();
		iter.prev();
		return iter.eof() ? null : db.input(iter.keyoff());
	}

	protected void check(int... values) {
		check("test", values);
	}

	protected void check(String tablename, int... values) {
			Transaction t = db.readonlyTran();
			check(t, tablename, values);
			t.ck_complete();
		}

	protected void check(Transaction t, String filename, int... values) {
			List<Record> recs = get(filename, t);
			assertEquals("number of values", values.length, recs.size());
			for (int i = 0; i < values.length; ++i)
				assertEquals(record(values[i]), recs.get(i));
		}

	protected static Record record(int i) {
		return new Record().add(i).add("more stuff");
	}

}