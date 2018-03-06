/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.immudb.Table;
import suneido.database.immudb.Transaction;

public class RuleFieldTest extends TestBase {

	@Override
	protected void makeDB() {
	}

	@Test
	public void rule_fields_not_saved() {
		adm("create withrule (a,B) key(a)");

		Transaction t = db.readTransaction();
		Table tbl = t.ck_getTable("withrule");
		assertEquals("[b, a]", tbl.getColumns().toString());
		t.complete();

		Query q = CompileQuery.query(db, serverData, "withrule");
		Header hdr = q.header();
		assertEquals("[b, a]", hdr.columns().toString());
		assertEquals("[a]", hdr.fields().toString());
		assertEquals("[b]", hdr.rules().toString());

		req("insert { a: 1, b: 2 } into withrule");
	}

	@Test
	public void misc() {
		// rule fields are stored with a field number of -1
		Record r = new RecordBuilder().add(-1).build();
		assertEquals(-1, r.getInt(0));
		Record r2 = new RecordBuilder().add(0).build();
		assertTrue(r.compareTo(r2) < 0);
	}

}
