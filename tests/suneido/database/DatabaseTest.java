package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

public class DatabaseTest {
	Database db;

	@Test
	public void test() {
		new File("databasetest").delete();
		db = new Database("databasetest", Mode.CREATE);
		String b = "hello";
		Record r = new Record().add(b);
		long offset = db.output(1234, r);

		db.close();
		db = new Database("databasetest", Mode.OPEN);

		Record r2 = db.input(offset);
		assertEquals(r, r2);

		ByteBuffer bb = db.adr(offset - 4);
		assertEquals(1234, bb.getInt());

		Table tbl = db.getTable("indexes");
		assertEquals("indexes", tbl.name);
		assertSame(tbl, db.getTable(2));

		db.addTable("test");
		db.getTable("test");

		db.addColumn("test", "a");
		db.addColumn("test", "b");

		db.addIndex("test", "a", true, "", "", 0, false, false);

		tbl = db.getTable("test");
		assertEquals(2, tbl.columns.size());
		assertEquals(1, tbl.indexes.size());

		r = new Record().add("12").add("34");
		db.add_any_record(0, "test", r);

		db.close();
		db = new Database("databasetest", Mode.OPEN);

		tbl = db.getTable("test");
		assertEquals(1, tbl.nrecords);

		Index idx = tbl.indexes.first();
		BtreeIndex.Iter iter = idx.btreeIndex.iter(0).next();
		r2 = iter.data();
		assertEquals(r, r2);
		iter.next();
		assertTrue(iter.eof());
	}

	@After
	public void close() {
		if (db != null)
			db.close();
	}

	@AfterClass
	public static void cleanup() {
		new File("databasetest").delete();
	}

	// public static void main(String args[]) {
	// new DatabaseTest().create_open();
	// }
}
