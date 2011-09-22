/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import suneido.Suneido;

public class TestBase {
	protected Database db = dbpkg().testdb();

	protected DatabasePackage dbpkg() {
		return Suneido.dbpkg;
	}

	protected Record record(int i) {
		return dbpkg().recordBuilder().add(i).add("more stuff").build();
	}

	protected Table getTable(String tableName) {
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable(tableName);
		t.complete();
		return tbl;
	}

	protected int getNrecords(String tableName) {
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable(tableName);
		int n = t.tableCount(tbl.num());
		t.complete();
		return n;
	}

	protected void makeTable() {
		makeTable(0);
	}

	protected void makeTable(int nrecords) {
		makeTable("test", nrecords);
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

	private void addRecords(String tablename, int from, int to) {
		while (from <= to) {
			Transaction t = db.readwriteTran();
			for (int i = 0; i < 1000 && from <= to; ++i, ++from)
				t.addRecord(tablename, record(from));
			t.ck_complete();
		}
	}

	protected Record record(int... args) {
		RecordBuilder rb = dbpkg().recordBuilder();
		for (int arg : args)
			rb.add(arg);
		return rb.build();
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

	private List<Record> get(String tablename, Transaction tran) {
		List<Record> recs = new ArrayList<Record>();
		Table tbl = tran.getTable(tablename);
		IndexIter iter = tran.iter(tbl.num(), null);
		for (iter.next(); ! iter.eof(); iter.next())
			recs.add(tran.input(iter.keyadr()));
		return recs;
	}

	protected Record getFirst(String tablename, Transaction tran) {
		Table tbl = tran.getTable(tablename);
		IndexIter iter = tran.iter(tbl.num(), null);
		iter.next();
		return iter.eof() ? null : tran.input(iter.keyadr());
	}

	protected Record getLast(String tablename, Transaction tran) {
		Table tbl = tran.getTable(tablename);
		IndexIter iter = tran.iter(tbl.num(), null);
		iter.prev();
		return iter.eof() ? null : tran.input(iter.keyadr());
	}

	protected int count(String tablename) {
		Transaction tran = db.readonlyTran();
		int n = count(tablename, tran);
		tran.ck_complete();
		return n;
	}

	private int count(String tablename, Transaction tran) {
		int n = 0;
		Table tbl = tran.getTable(tablename);
		IndexIter iter = tran.iter(tbl.num(), null);
		for (iter.next(); ! iter.eof(); iter.next())
			n++;
		return n;
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

	protected Record key(int i) {
		return dbpkg().recordBuilder().add(i).build();
	}

}