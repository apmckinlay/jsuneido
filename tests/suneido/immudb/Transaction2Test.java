/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.intfc.database.IndexIter;

public class Transaction2Test {

	@Test
	public void bootstrap() {
		Storage stor = new MemStorage(1024, 1024);
		Database2.create(stor);
	}

	@Test
	public void read_tables() {
		Storage stor = new MemStorage(1024, 1024);
		Database.create(stor).close();
		Database2 db = Database2.open(stor);
		check(db.readonlyTran());
		check(db.readwriteTran());
		check(db.exclusiveTran());
	}
	private static void check(ImmuReadTran t) {
		Record[] recs = { rec(1, "tables"), rec(2, "columns"), rec(3, "indexes"),
				rec(4, "views") };
		check(t, Bootstrap.TN.TABLES, recs);
	}

	@Test
	public void add_remove() {
		Storage stor = new MemStorage(1024, 1024);
//		Database db1 = Database.create(stor);
//		db1.createTable("tmp")
//			.addColumn("a")
//			.addColumn("b")
//			.addIndex("a", true, false, null, null, 0)
//			.finish();
//		db1.close();

//		Database2 db = Database2.open(stor);
		Database2 db = Database2.create(stor);
		db.createTable("tmp")
				.addColumn("a")
				.addColumn("b")
				.addIndex("a", true, false, null, null, 0)
				.finish();

		ImmuUpdateTran t = db.readwriteTran();
		int tblnum = t.getTable("tmp").num;
		assertThat(t.tableCount(tblnum), is(0));
		assertThat(t.tableSize(tblnum), is(0L));
		t.addRecord("tmp", rec(123, "foo"));
		assertThat(t.tableCount(tblnum), is(1));
		assertThat(t.tableSize(tblnum), is(15L));
		assertNotNull(t.lookup(tblnum, new int[] { 0 }, rec(123)));
		check(t, "tmp", rec(123, "foo"));
		t = null;

		ImmuReadTran rt = db.readonlyTran();
		assertThat(rt.tableCount(tblnum), is(1));
		assertThat(rt.tableSize(tblnum), is(15L));
		Record r = rt.lookup(tblnum, new int[] { 0 }, rec(123));
		rt.complete();
		rt = null;

		check(db.readonlyTran(), "tmp", rec(123, "foo"));

		t = db.readwriteTran();
		r = t.lookup(tblnum, new int[] { 0 }, rec(123));
		t.removeRecord(tblnum, r);
		assertThat(t.tableCount(tblnum), is(0));
		assertThat(t.tableSize(tblnum), is(0L));
		assertNull(t.lookup(tblnum, new int[] { 0 }, rec(123)));
		check(t, "tmp");
		t = null;

		t = db.readwriteTran();
		assertThat(t.tableCount(tblnum), is(0));
		assertThat(t.tableSize(tblnum), is(0L));
		t.addRecord(tblnum, rec(456, "bar"));
		r = t.lookup(tblnum, new int[] { 0 }, rec(456));
		t.removeRecord(tblnum, r);
		check(t, "tmp");
		t = null;

		check(db.readonlyTran(), "tmp");
	}

	private static void check(ImmuReadTran t, String tableName, Record... recs) {
		Table tbl = t.getTable(tableName);
		check(t, tbl.num, recs);
	}

	private static void check(ImmuReadTran t, int tblnum, Record... recs) {
		int i = 0;
		IndexIter iter = t.iter(tblnum, null);
		for (iter.next(); ! iter.eof(); iter.next(), ++i)
			assertThat(t.input(iter.keyadr()), is(recs[i]));
		assertThat(i, is(recs.length));
		t.complete();
	}

	static Record rec(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object val : values)
			if (val instanceof Integer)
				rb.add(((Integer) val).intValue());
			else
				rb.add(val);
		return rb.build();
	}

}