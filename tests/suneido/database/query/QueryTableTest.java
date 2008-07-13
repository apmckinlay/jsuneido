package suneido.database.query;

import static suneido.database.query.Query.Dir.NEXT;

import org.junit.Test;

import suneido.database.TestBase;
import suneido.database.Transaction;

public class QueryTableTest extends TestBase {

	@Test
	public void test() {
		makeTable(3);

		QueryTable qt = new QueryTable("test");
		qt.optimize2(null, null, null, false, false); // TEMPORARY
		Transaction t = db.readonlyTran();
		qt.setTransaction(t);
		Row row = qt.get(NEXT);
	}
}
