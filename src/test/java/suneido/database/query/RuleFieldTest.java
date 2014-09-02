/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.Suneido.dbpkg;

import org.junit.Test;

import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class RuleFieldTest extends TestBase {

	@Override
	protected void makeDB() {
	}

	@Test
	public void rule_fields_not_saved() {
		adm("create withrule (a,B) key(a)");

		Transaction t = db.readTransaction();
		suneido.intfc.database.Table tbl = t.ck_getTable("withrule");
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
		Record r = dbpkg.recordBuilder().add(-1).build();
		assertEquals(-1, r.getInt(0));
		Record r2 = dbpkg.recordBuilder().add(0).build();
		assertTrue(r.compareTo(r2) < 0);
	}

}
