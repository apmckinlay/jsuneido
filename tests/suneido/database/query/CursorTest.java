package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static suneido.database.Database.theDB;

import org.junit.Test;

import suneido.database.Transaction;
import suneido.database.query.Query.Dir;

public class CursorTest extends TestBase {

	@Test
	public void test() {
		Transaction t;
		Query q;

		t = theDB.readonlyTran();
		q = CompileQuery.query(t, serverData, "customer", true);
		t.complete();

		t = theDB.readonlyTran();
		q = CompileQuery.query(t, serverData, "customer", true);
		Row row1 = q.get(Dir.NEXT);
		Row row2 = q.get(Dir.NEXT);
		t.complete();

		t = theDB.readonlyTran();
		q = CompileQuery.query(t, serverData, "customer", true);
		t.complete();

		t = theDB.readonlyTran();
		q.setTransaction(t);
		assertEquals(row1, q.get(Dir.NEXT));
		t.complete();

		t = theDB.readonlyTran();
		q.setTransaction(t);
		assertEquals(row2, q.get(Dir.NEXT));
		t.complete();

		q.rewind();

		t = theDB.readonlyTran();
		q.setTransaction(t);
		assertEquals(row1, q.get(Dir.NEXT));
		t.complete();

		req("delete customer");

		t = theDB.readonlyTran();
		q.setTransaction(t);
		assertNull(q.get(Dir.PREV));
		t.complete();
	}

}
