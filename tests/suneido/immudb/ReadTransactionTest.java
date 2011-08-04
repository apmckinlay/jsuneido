/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.intfc.database.Transaction;

public class ReadTransactionTest {

	@Test
	public void lookup() {
		Database db = new DatabasePackage().testdb();
		Transaction t = db.readonlyTran();
		Record key = new RecordBuilder().add("indexes").build();
		Record r = (Record) t.lookup(1, "tablename", key);
		assertThat(r.getString(1), is("indexes"));

		key = new RecordBuilder().add("fred").build();
		r = (Record) t.lookup(1, "tablename", key);
		assertNull(r);
	}

}
