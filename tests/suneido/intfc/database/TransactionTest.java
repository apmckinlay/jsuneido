/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransactionTest extends TestBase {

	@Test
	public void cleanup() {
		db.tranlist().isEmpty();

		Transaction t1 = db.readonlyTran();
		getFirst("tables", t1);
		t1.ck_complete();

		db.tranlist().isEmpty();

		makeTable(3);

		db.tranlist().isEmpty();

		Transaction t2 = db.readwriteTran();
		t2.ck_complete();

		db.tranlist().isEmpty();

		Transaction t3 = db.readwriteTran();
		getFirst("test", t3);
		t3.ck_complete();

		db.tranlist().isEmpty();
	}

	@Test
	public void visibility() {
		db.tranlist().isEmpty();

		makeTable(1000);
		before = new int[] { 0, 1, 2, 997, 998, 999 };
		after = new int[] { 1, 2, 3, 998, 999, 9999 };

		Transaction t1 = add_remove();

		// uncommitted NOT visible to other transactions
		Transaction t2 = db.readonlyTran();
		checkBefore(t2);
		Transaction t3 = db.readwriteTran();
		checkBefore(t3);

		t1.ck_complete();

		// committed updates visible to later transactions
		Transaction t4 = db.readonlyTran();
		checkAfter(t4);
		t4.ck_complete();
		Transaction t5 = db.readwriteTran();
		checkAfter(t5);
		t5.ck_complete();

		// earlier transactions still don't see changes
		// (transactions see data as of their start time)
		checkBefore(t2);
		t2.ck_complete();
		checkBefore(t3);
		t3.ck_complete();

		db.tranlist().isEmpty();
	}

	@Test
	public void abort() {
		makeTable(4);
		before = new int[] { 0, 1, 2, 3 };
		after = new int[] { 1, 2, 3, 9999 };

		Transaction t1 = add_remove();
		t1.abort(); // should abort outstanding transactions

		// aborted updates not visible
		checkBefore();

		db.tranlist().isEmpty();

		db.reopen();

		// aborted updates still not visible
		checkBefore();

		db.tranlist().isEmpty();
	}

	private Transaction add_remove() {
		Transaction t = db.readwriteTran();
		checkBefore(t);
		t.addRecord("test", record(9999));
		t.removeRecord(t.getTable("test").num(), getFirst("test", t));
		checkAfter(t); // uncommitted ARE visible to their transaction
		return t;
	}

	private int[] before;

	private void checkBefore() {
		check(before);
	}

	private void checkBefore(Transaction t) {
		checkEnds(t, before);
	}

	private int[] after;

	private void checkAfter(Transaction t) {
		checkEnds(t, after);
	}

	protected void checkEnds(Transaction t, int... values) {
		Table tbl = t.getTable("test");
		IndexIter iter = t.iter(tbl.num(), null);
		iter.next();
		for (int i = 0; i < values.length / 2; iter.next(), ++i) {
			Record r = t.input(iter.keyadr());
			assertEquals(record(values[i]), r);
		}
		iter = t.iter(tbl.num(), null);
		iter.prev();
		for (int i = values.length - 1; i >= values.length / 2; iter.prev(), --i) {
			Record r = t.input(iter.keyadr());
			assertEquals(record(values[i]), r);
		}
	}

//	@Test
//	public void delete_conflict() {
//		makeTable(1000);
//
//		// deleting different btree nodes doesn't conflict
//		Transaction t1 = db.readwriteTran();
//		t1.removeRecord("test", "a", key(1));
//		Transaction t2 = db.readwriteTran();
//		t2.removeRecord("test", "a", key(999));
//		t2.ck_complete();
//		t1.ck_complete();
//
//		db.tranlist().isEmpty();
//
//		// deleting from the same btree node conflicts
//		Transaction t3 = db.readwriteTran();
//		t3.removeRecord("test", "a", key(4));
//		Transaction t4 = db.readwriteTran();
//		try {
//			t4.removeRecord("test", "a", key(5));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assertTrue(t4.isAborted());
//		t3.ck_complete();
//		assertNotNull(t4.complete());
//		assertTrue(t4.conflict().contains("conflict (write-write)"));
//
//		db.tranlist().isEmpty();
//	}

//	@Test
//	public void add_conflict() {
//		makeTable();
//
//		Transaction t1 = db.readwriteTran();
//		t1.addRecord("test", record(99));
//		Transaction t2 = db.readwriteTran();
//		try {
//			t2.addRecord("test", record(99));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		t1.ck_complete();
//		assertNotNull(t2.complete());
//		assertTrue(t2.conflict().contains("conflict (write-write)"));
//
//		db.tranlist().isEmpty();
//	}

//	@Test
//	public void update_conflict() {
//		makeTable(3);
//
//		Transaction t1 = db.readwriteTran();
//		t1.updateRecord("test", "a", key(1), record(5));
//
//		Transaction t2 = db.readwriteTran();
//		try {
//			t2.updateRecord("test", "a", key(1), record(6));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assert(t2.isAborted());
//		assertNotNull(t2.complete());
//		assertTrue(t2.conflict().contains("conflict (write-write)"));
//
//		Transaction t3 = db.readwriteTran();
//		try {
//			t3.updateRecord("test", "a", key(1), record(6));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assert(t3.isAborted());
//		assertNotNull(t3.complete());
//		assertTrue(t3.conflict().contains("conflict (write-write)"));
//
//		t1.ck_complete();
//
//		db.tranlist().isEmpty();
//	}
//
//	@Test
//	public void update_conflict_2() {
//		makeTable(3);
//
//		Transaction t1 = db.readwriteTran();
//
//		Transaction t2 = db.readwriteTran();
//		t2.updateRecord("test", "a", key(1), record(6));
//		t2.ck_complete();
//
//		try {
//			t1.updateRecord("test", "a", key(1), record(6));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assert(t1.isAborted());
//		assertNotNull(t1.complete());
//		assertTrue(t1.conflict().contains("conflict (write-write)"));
//
//		db.tranlist().isEmpty();
//	}

	@Test
	public void update_visibility() {
		makeTable(5);
		Transaction t = db.readwriteTran();
		t.updateRecord(t.getTable("test").num(), getFirst("test", t), record(9));
		check(0, 1, 2, 3, 4);
		t.ck_complete();
		check(1, 2, 3, 4, 9);

		db.tranlist().isEmpty();
	}

//	@Test
//	public void write_skew() {
//		makeTable(1000);
//		Transaction t1  = db.readwriteTran();
//		Transaction t2  = db.readwriteTran();
//
//		getFirst("test", t1);
//		getLast("test", t1);
//		t1.updateRecord("test", "a", key(1), record(1));
//
//		getFirst("test", t2);
//		getLast("test", t2);
//		try {
//			t2.updateRecord("test", "a", key(999), record(999));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assert(t2.isAborted());
//
//		t1.ck_complete();
//		assertNotNull(t2.complete());
//		assertStartsWith("conflict (write-read)", t2.conflict());
//
//		db.tranlist().isEmpty();
//	}
//
//	private void assertStartsWith(String expectedPrefix, String s) {
//		assertTrue("expected to startWith: <" + expectedPrefix + ">" +
//				" but was <" + s + ">",
//				s.startsWith(expectedPrefix));
//	}
//
//	@Test
//	public void phantoms() {
//		makeTable(1000);
//		Transaction t1  = db.readwriteTran();
//		getLast("test", t1);
//		t1.updateRecord("test", "a", key(1), record(1));
//
//		Transaction t2  = db.readwriteTran();
//		try {
//			t2.addRecord("test", record(1000));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assertTrue(t2.isAborted());
//
//		Transaction t3  = db.readwriteTran();
//		try {
//			t3.removeRecord("test", "a", key(999));
//			fail();
//		} catch (SuException e) {
//			assertTrue(e.toString().contains("conflict"));
//		}
//		assertTrue(t3.isAborted());
//
//		t1.ck_complete();
//
//		db.tranlist().isEmpty();
//	}

	@Test
	public void nested() {
		makeTable("test1");
		makeTable("test2");

		Transaction t1 = db.readwriteTran();
		t1.addRecord("test1", record(123));

		Transaction t2 = db.readwriteTran();
		t2.addRecord("test2", record(456));
		t2.ck_complete();

		t1.ck_complete();

		check("test1", 123);
		check("test2", 456);

		db.tranlist().isEmpty();
	}

}
