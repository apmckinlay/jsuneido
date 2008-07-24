package suneido.database.query;

import static suneido.database.query.Query.noFields;
import static suneido.database.query.Query.Dir.NEXT;

import org.junit.Test;

import suneido.database.TestBase;
import suneido.database.Transaction;

public class TableTest extends TestBase {

	@Test
	public void test() {
		makeTable(3);

		Table qt = new Table("test");
		qt.optimize2(noFields, noFields, noFields, false, false); // TEMPORARY
		Transaction t = db.readonlyTran();
		qt.setTransaction(t);
		Row row = qt.get(NEXT);
	}
}
