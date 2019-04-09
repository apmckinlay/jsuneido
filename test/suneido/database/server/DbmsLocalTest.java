/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;

public class DbmsLocalTest {
	@Test
	public void test() {
		Dbms dbms = new DbmsLocal(Dbpkg.testdb());

		dbms.admin("create test (a, b, c) key(a)");

		DbmsTran t1 = dbms.transaction(true);
		t1.request("insert { a: 1, b: 2, c: 3 } into test");
		t1.complete();

		HeaderAndRow hr = dbms.get(Dir.NEXT, "test", true);
		assertEquals(1, hr.row.getval(hr.header, "a"));

		DbmsTran t2 = dbms.transaction(true);
		DbmsQuery q = t2.query("test");
		q.output(new RecordBuilder().add(4).add(5).add(6).build());
		q.rewind();
		Row row = q.get(Dir.PREV);
		Header hdr = q.header();
		t2.complete();
		assertEquals(6, row.getval(hdr, "c"));
	}
}
