package suneido.database.server;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;

import org.junit.Test;

import suneido.SuInteger;
import suneido.database.*;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;

public class DbmsLocalTest {
	@Test
	public void test() {
		theDB = new Database(new DestMem(), Mode.CREATE);
		Dbms dbms = new DbmsLocal();

		dbms.admin("create test (a, b, c) key(a)");

		int tn1 = dbms.transaction(true, "");
		dbms.request(tn1, "insert { a: 1, b: 2, c: 3 } into test");
		dbms.complete(tn1);

		HeaderAndRow hr = dbms.get(Dir.NEXT, "test", true, 0);
		assertEquals(SuInteger.valueOf(1), hr.row.getval(hr.header, "a"));

		int tn2 = dbms.transaction(true, "");
		DbmsQuery q = dbms.query(tn2, "test");
		q.output(new Record().add(SuInteger.valueOf(4))
				.add(SuInteger.valueOf(5)).add(SuInteger.valueOf(6)));
		q.rewind();
		Row row = q.get(Dir.PREV);
		Header hdr = q.header();
		dbms.complete(tn2);
		assertEquals(SuInteger.valueOf(6), row.getval(hdr, "c"));
	}
}
