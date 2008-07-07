package suneido.database.query;

import static suneido.database.Database.theDB;
import static suneido.database.query.Query.Dir.NEXT;

import org.junit.Test;

import suneido.database.TestBase;
import suneido.database.Transaction;


public class QueryTableTest extends TestBase {

	@Test
	public void test() {
		theDB = db;
		makeTable(3);

		QueryTable qt = new QueryTable("test");
		Transaction t = db.readonlyTran();
		qt.setTransaction(t);
		Row row = qt.get(NEXT);
	}
}
