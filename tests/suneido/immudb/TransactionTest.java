/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suneido.immudb.Bootstrap.TN;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.intfc.database.Transaction;

public class TransactionTest {

	@Test
	public void lookup() {
		ImmuDatabase db = DatabasePackage.dbpkg.testdb();
		Transaction t = db.readonlyTran();
		Record key = new RecordBuilder().add("indexes").build();
		Record r = (Record) t.lookup(1, "tablename", key);
		assertThat(r.getString(1), is("indexes"));

		key = new RecordBuilder().add("fred").build();
		r = (Record) t.lookup(1, "tablename", key);
		assertNull(r);
	}

	@Test
	public void exclusive_abort() {
		Database db = (Database) DatabasePackage.dbpkg.testdb();
		db.exclusiveTran().abort();
		assertThat(db.check(), is(Status.OK));
	}

	@Test
	public void locking() {
		Database db = (Database) DatabasePackage.dbpkg.testdb();
		db.checkTransEmpty();
		UpdateTransaction t = db.readwriteTran();
		t.ck_complete();
		db.checkTransEmpty();

		t = db.readwriteTran();
		assertNotNull(t.lookup(TN.TABLES, Bootstrap.indexColumns[TN.TABLES],
				new RecordBuilder().add(TN.TABLES).build()));
		assertTrue(! db.trans.isLocksEmpty());
		t.ck_complete();
		assertTrue(t.isCommitted());
		db.checkTransEmpty();

		UpdateTransaction t2 = db.readwriteTran();
		t = db.readwriteTran();
		assertNotNull(t.lookup(TN.TABLES, Bootstrap.indexColumns[TN.TABLES],
				new RecordBuilder().add(TN.TABLES).build()));
		t.ck_complete();
		assertTrue(! db.trans.isLocksEmpty()); // waiting for finalization
		t2.ck_complete();
		db.checkTransEmpty();
	}

}
