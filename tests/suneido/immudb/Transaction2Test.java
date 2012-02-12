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
		check(db.exclusiveTran());
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

		t.complete();
	}

	private void check(ImmuReadTran t) {
		IndexIter iter = t.iter(Bootstrap.TN.TABLES, "table");
		Record[] recs = { rec(1, "tables"), rec(2, "columns"), rec(3, "indexes"),
				rec(4, "views") };
		int i = 0;
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
