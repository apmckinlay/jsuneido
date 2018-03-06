/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static suneido.database.immudb.Dbpkg.nullObserver;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import suneido.database.immudb.Dbpkg.Status;
import suneido.util.FileUtils;

public class DbRebuild2Test extends TestBase {
	protected String filename;
	protected String outfilename;

	@Before
	public void create() {
		File file = FileUtils.tempfile("d", "i", "c");
		filename = file.toString();
		outfilename = filename + ".out";
		db = Dbpkg.create(filename);
	}

	@After
    public void delete() {
    	new File(filename).deleteOnExit();
    	new File(outfilename).deleteOnExit();
    }

	@Test
	public void test_empty_database() {
		rebuild();
	}

	@Test
	public void test_empty_table() {
		makeTable(0);
		rebuild();
		checkEmpty();
	}

	@Test
	public void test_add() {
		makeTable(4);
		rebuild();
		checkTable();
	}

	@Test
	public void test_add_remove() {
		makeTable(6);
		Transaction t = db.updateTransaction();
		Table tbl = t.getTable("test");
		t.removeRecord(tbl.num(), t.lookup(tbl.num(), "a", key(4)));
		t.removeRecord(tbl.num(), t.lookup(tbl.num(), "a", key(5)));
		t.ck_complete();
		rebuild();
		checkTable();
	}

	@Test
	public void test_add_index_with_existing_records() {
		makeTable(4);
		db.alterTable("test").addIndex("b", false, false, null, null, 0).finish();
		rebuild();
		checkTable();
	}

	@Test
	public void test_drop() {
		makeTable(4);
		db.dropTable("test");
		rebuild();
		checkNoTable();
	}

	@Test
	public void test_drop_recreate() {
		makeTable(4);
		db.dropTable("test");
		makeTable(4);
		rebuild();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		makeTable("tmp", 3);
		db.dropTable("tmp"); // so new database has different offsets
		makeTable("before", 2);
		db.renameTable("before", "test");
		addRecords("test", 2, 3);
		rebuild();
		checkTable();
	}

	@Test
	public void test_rename_column() {
		makeTable("tmp", 3);
		db.dropTable("tmp"); // so new database has different offsets
		makeTable(2);
		assertThat(db.getSchema("test"), equalTo("(a,b) key(a) index(b,a)"));
		db.alterTable("test").renameColumn("b", "bb").finish();
		addRecords("test", 2, 3);
		assertThat(db.getSchema("test"), equalTo("(a,bb) key(a) index(bb,a)"));
		rebuild();
		db = Dbpkg.openReadonly(outfilename);
		assertThat(db.getSchema("test"), equalTo("(a,bb) key(a) index(bb,a)"));
		checkTable();
	}

	@Test
	public void test_update() {
		makeTable(4);
		Transaction t = db.updateTransaction();
		Table tbl = t.getTable("test");
		Record oldrec = t.lookup(tbl.num(), "a", key(2));
		t.updateRecord(tbl.num(), oldrec, record(99));
		t.ck_complete();
		rebuild();
		checkTable(0, 1, 3, 99);
	}

	@Test
	public void test_add_view() {
		db.addView("myview", "indexes");
		rebuild();
		db = Dbpkg.openReadonly(outfilename);
		Transaction t = db.readTransaction();
		assertEquals("indexes", t.getView("myview"));
		t.ck_complete();
	}

	@Test
	public void test_drop_view() {
		db.addView("myview", "indexes");
		db.dropTable("myview");
		rebuild();
		db = Dbpkg.openReadonly(outfilename);
		Transaction t = db.readTransaction();
		assertNull(t.getView("myview"));
		t.ck_complete();
	}

	// -------------------------------------------------------------------------

	private void rebuild() {
		db.close();
		db = null;
		assertNotNull(Dbpkg.rebuildFromData(filename, outfilename));
		assertEquals(Status.OK, Dbpkg.check(outfilename, nullObserver));
	}

	private void checkTable() {
		checkTable(0, 1, 2, 3);
	}

	private void checkTable(int... values) {
		if (db == null)
			db = Dbpkg.openReadonly(outfilename);
		try {
			check(values);
		} finally {
			db.close();
		}
	}

	private void checkNoTable() {
		if (db == null)
			db = Dbpkg.openReadonly(outfilename);
		Transaction t = db.readTransaction();
		assertNull(t.getTable("test"));
		t.ck_complete();
	}

	private void checkEmpty() {
		if (db == null)
			db = Dbpkg.openReadonly(outfilename);
		assertThat(count(), equalTo(0));
	}

}
