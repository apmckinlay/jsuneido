package suneido.database.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suneido.Transaction;
import suneido.Transaction.Table;
import suneido.database.BtreeIndex;
import suneido.database.Record;

public class UpdateTest extends TestBase {

	@Test
	public void test() {
		makeTable(4);

		assertEquals(4, get("test").size());
		assertEquals(2, req("update test where a >= 1 and a <= 2 set b = 'xxx'"));
		List<Record> recs = get("test");
		assertEquals(4, recs.size());
		assertEquals(record(0), recs.get(0));
		assertEquals(record(3), recs.get(3));
		assertEquals(new Record().add(1).add("xxx"), recs.get(1));
		assertEquals(new Record().add(2).add("xxx"), recs.get(2));
	}

	@Test
	public void test2() {
		Request.execute(db, "create lib " +
				"(group, lib_committed, lib_modified, name, num, parent, text) " +
				"key (name,group) " +
				"key (num) " +
				"index (parent) " +
				"index (parent,name)");
		for (int i = 0; i < 10; ++i) {
			Record rec = mkrec(i);
			Transaction t = db.readwriteTran();
			t.addRecord("lib", rec);
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
		Table table = t.getTable("lib");
		String[] indexes = { "name,group", "num", "parent", "parent,name" };
		for (String cols : indexes) {
			int n = 0;
			BtreeIndex bti = t.getBtreeIndex(table.num(), cols);
			BtreeIndex.Iter iter = bti.iter().next();
			for (; !iter.eof(); iter.next())
				++n;
			assertEquals(10, n);
		}
		t.ck_complete();
	}

}
