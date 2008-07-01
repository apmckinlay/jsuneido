package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static suneido.database.Transaction.NULLTRAN;

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

		Transaction t = db.readwriteTran();
		db.addTable("test");
		db.ck_getTable("test");

		db.addColumn("test", "a");
		db.addColumn("test", "b");

		db.addIndex("test", "a", true, false, false, "", "", 0);

		tbl = db.getTable("test");
		assertEquals(2, tbl.columns.size());
		assertEquals(1, tbl.indexes.size());

		r = new Record().add(12).add(34);
		t = db.readwriteTran();
		try {
			db.addRecord(t, "test", r);
		} finally {
			assertTrue(t.complete());
		}

		db.close();
		db = new Database("databasetest", Mode.OPEN);

		tbl = db.getTable("test");
		assertEquals(1, tbl.nrecords);

		Index idx = tbl.indexes.first();
		BtreeIndex.Iter iter = idx.btreeIndex.iter(NULLTRAN).next();
		r2 = iter.data();
		assertEquals(r, r2);
		iter.next();
		assertTrue(iter.eof());

		t = db.readwriteTran();
		try {
			db.removeRecord(t, "test", "a", new Record().add(12));
		} finally {
			assertTrue(t.complete());
		}
		iter = idx.btreeIndex.iter(NULLTRAN).next();
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

	 public static void main(String args[]) {
		new DatabaseTest().test();
	}
}
