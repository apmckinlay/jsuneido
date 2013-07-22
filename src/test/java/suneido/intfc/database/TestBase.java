/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import suneido.Suneido;

public class TestBase {
	private static final String DEFAULT_TABLENAME = "test";
	protected Database db = dbpkg().testdb();

	protected DatabasePackage dbpkg() {
		return Suneido.dbpkg;
	}

	protected Record record(int i) {
		return dbpkg().recordBuilder().add(i).add("more stuff").build();
	}

	protected Table getTable(String tableName) {
		Transaction t = db.readTransaction();
		Table tbl = t.getTable(tableName);
		t.complete();
		return tbl;
	}

	protected int getNrecords(String tableName) {
		Transaction t = db.readTransaction();
		Table tbl = t.getTable(tableName);
		int n = t.tableCount(tbl.num());
		t.complete();
		return n;
	}

	protected void makeTable() {
		makeTable(0);
	}

	protected void makeTable(int nrecords) {
		makeTable(DEFAULT_TABLENAME, nrecords);
	}

	protected void makeTable(String tablename) {
		makeTable(tablename, 0);
	}

	protected void makeTable(String tablename, int nrecords) {
		db.createTable(tablename)
			.addColumn("a")
			.addColumn("b")
			.addIndex("a", true, false, "", "", 0)
			.addIndex("b,a", false, false, "", "", 0)
			.finish();
		addRecords(tablename, 0, nrecords - 1);
	}

	protected void addRecords(String tablename, int from, int to) {
		while (from <= to) {
			Transaction t = db.updateTransaction();
			for (int i = 0; i < 1000 && from <= to; ++i, ++from)
				t.addRecord(tablename, record(from));
			t.ck_complete();
		}
	}

	protected Record rec(Object... values) {
		RecordBuilder rb = dbpkg().recordBuilder();
		for (Object val : values)
			if (val instanceof Integer)
				rb.add(((Integer) val).intValue());
			else
				rb.add(val);
		return rb.build();
	}


	protected List<Record> get() {
		return get(DEFAULT_TABLENAME);
	}

	protected List<Record> get(String tablename) {
		Transaction t = db.readTransaction();
		List<Record> recs = get(tablename, t);
		t.ck_complete();
		return recs;
	}

	protected List<Record> get(Transaction t) {
		return get(DEFAULT_TABLENAME, t);
	}

	private static List<Record> get(String tablename, Transaction t) {
		List<Record> recs = new ArrayList<Record>();
		Table tbl = t.getTable(tablename);
		IndexIter iter = t.iter(tbl.num(), null);
		for (iter.next(); ! iter.eof(); iter.next())
			recs.add(t.input(iter.keyadr()));
		return recs;
	}

	protected Record getFirst(String tablename, Transaction t) {
		Table tbl = t.getTable(tablename);
		IndexIter iter = t.iter(tbl.num(), null);
		iter.next();
		return iter.eof() ? null : t.input(iter.keyadr());
	}

	protected Record getLast(String tablename, Transaction t) {
		Table tbl = t.getTable(tablename);
		IndexIter iter = t.iter(tbl.num(), null);
		iter.prev();
		return iter.eof() ? null : t.input(iter.keyadr());
	}

	protected int count() {
		return count(DEFAULT_TABLENAME);
	}

	protected int count(String tablename) {
		Transaction t = db.readTransaction();
		int n = count(tablename, t);
		t.ck_complete();
		return n;
	}

	private static int count(String tablename, Transaction t) {
		int n = 0;
		Table tbl = t.getTable(tablename);
		assert tbl != null : "nonexistent table: " + tablename;
		IndexIter iter = t.iter(tbl.num(), null);
		for (iter.next(); ! iter.eof(); iter.next())
			n++;
		return n;
	}

	protected void check(int... values) {
		check(DEFAULT_TABLENAME, values);
	}

	protected void check(String tablename, int... values) {
		Transaction t = db.readTransaction();
		check(t, tablename, values);
		t.ck_complete();
	}

	protected void check(Transaction t, String filename, int... values) {
		List<Record> recs = get(filename, t);
		assertEquals("number of values", values.length, recs.size());
		for (int i = 0; i < values.length; ++i)
			assertEquals(record(values[i]), recs.get(i));
	}

	protected Record key(int i) {
		return dbpkg().recordBuilder().add(i).build();
	}

}