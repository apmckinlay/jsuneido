package suneido.database.server;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;

import org.junit.Test;

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
		ServerData serverData = new ServerData();

		dbms.admin(null, "create test (a, b, c) key(a)");

		DbmsTran t1 = dbms.transaction(true, "");
		dbms.request(serverData, t1, "insert { a: 1, b: 2, c: 3 } into test");
		t1.complete();

		HeaderAndRow hr = dbms.get(serverData, Dir.NEXT, "test", true, null);
		assertEquals(1, hr.row.getval(hr.header, "a"));

		DbmsTran t2 = dbms.transaction(true, "");
		DbmsQuery q = dbms.query(serverData, t2, "test");
		q.output(new Record().add(4).add(5).add(6));
		q.rewind();
		Row row = q.get(Dir.PREV);
		Header hdr = q.header();
		t2.complete();
		assertEquals(6, row.getval(hdr, "c"));
	}
}
