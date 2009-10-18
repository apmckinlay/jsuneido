package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;

import java.util.List;

import org.junit.Test;

import suneido.database.*;
import suneido.database.Table;
import suneido.database.TestBase;

public class UpdateTest extends TestBase {

	@Test
	public void test() {
		makeTable(4);

		assertEquals(4, get("test").size());
		QueryAction q = (QueryAction) CompileQuery.parse(
				serverData, "update test where a >= 1 and a <= 2 set b = 'xxx'");
		q.setTransaction(new Transaction(theDB.tabledata));
		assertEquals(2, q.execute());
		List<Record> recs = get("test");
		assertEquals(4, recs.size());
		assertEquals(record(0), recs.get(0));
		assertEquals(record(3), recs.get(3));
		assertEquals(new Record().add(1).add("xxx"), recs.get(1));
		assertEquals(new Record().add(2).add("xxx"), recs.get(2));
	}

	@Test
	public void test2() {
		Request.execute("create lib " +
				"(group, lib_committed, lib_modified, name, num, parent, text) " +
				"key (name,group) " +
				"key (num) " +
				"index (parent) " +
				"index (parent,name)");
		for (int i = 0; i < 10; ++i) {
			Record rec = mkrec(i);
			Transaction t = db.readwriteTran();
			db.addRecord(t, "lib", rec);
			t.ck_complete();
		}

		checkCount();

		Transaction t = db.readwriteTran();
		// db.update(t, recadr, mkrec(5));
		t.ck_complete();

		checkCount();
	}

	private Record mkrec(int i) {
		return new Record().add(-1) // group
				.add("") // lib_committed
				.add("") // lib_modified
				.add("Foo" + i) // name
				.add(i) // num
				.add(45) // parent
				.add("now is the time\nfor all good\nmen"); // text
	}

	private void checkCount() {
		Transaction t = db.readonlyTran();
		Table table = db.getTable("lib");
		String[] indexes = { "name,group", "num", "parent", "parent,name" };
		for (String cols : indexes) {
			Index index = table.getIndex(cols);
			int n = 0;
			BtreeIndex.Iter iter = index.btreeIndex.iter(t).next();
			for (; !iter.eof(); iter.next())
				++n;
			assertEquals(10, n);
		}
		t.ck_complete();
	}

}
