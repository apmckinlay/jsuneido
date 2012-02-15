/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.intfc.database.IndexIter;

public class Transaction2Test {

	@Test
	public void read_tables() {
		Storage stor = new MemStorage(1024, 1024);
		Database.create(stor).close();
		Database2 db = Database2.open(stor);
		check(db.readonlyTran());
		check(db.readwriteTran());
//		check(db.exclusiveTran());
	}
	private static void check(ImmuReadTran t) {
		Record[] recs = { rec(1, "tables"), rec(2, "columns"), rec(3, "indexes"),
				rec(4, "views") };
		check(t, Bootstrap.TN.TABLES, recs);
	}

	@Test
	public void update() {
		Storage stor = new MemStorage(1024, 1024);
		Database db1 = Database.create(stor);
		db1.createTable("tmp")
			.addColumn("a")
			.addColumn("b")
			.addIndex("a", true, false, null, null, 0)
			.finish();
		db1.close();

		Database2 db = Database2.open(stor);
		ImmuUpdateTran t = db.readwriteTran();
		t.addRecord("tmp", rec(123, "foo"));
		check(t, "tmp", rec(123, "foo"));

//		check(db.readonlyTran(), "tmp", rec(123, "foo"));
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
