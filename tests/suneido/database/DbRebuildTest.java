package suneido.database;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import suneido.database.DbCheck.Status;


public class DbRebuildTest extends DbCheckRebuildTestBase {

	@Test
	public void test_empty() {
		new Database(filename, Mode.CREATE).close();
		dbrebuild();
		dbcheck();
	}

	@Test
	public void test_empty_table() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 0);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheck();
		checkTable(0);
	}

	@Test
	public void test_simple() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_add_index_with_existing_records() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			db.addIndex("mytable", "b", false);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_drop() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			db.dropTable("mytable");
		} finally {
			db.close();
		}
		checkNoTable();
		dbrebuild();
		dbcheck();
		checkNoTable();
	}

	@Test
	public void test_drop_recreate() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			db.dropTable("mytable");
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		checkTable();
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("tmp", 4);
			db.dropTable("tmp"); // so new theDB() has different offsets
			makeTable("mytable_before", 4);
			db.renameTable("mytable_before", "mytable");
			addRecords("mytable", 4, 7);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheck();
		checkTable(8);
	}

	@Test
	public void test_views() {
		db = new Database(filename, Mode.CREATE);
		try {
			db.addView("myview", "indexes");
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheck();
		db = new Database(filename, Mode.OPEN);
		try {
			Transaction t = db.readonlyTran();
			assertEquals("indexes", db.getView(t, "myview"));
		} finally {
			db.close();
		}
	}

	private void dbrebuild() {
		DbRebuild dbr = new DbRebuild(filename, outfilename, false);
		Status status = dbr.checkPrint();
		assertEquals(Status.OK, status);
		dbr.rebuild();
	}

	@After
	public void remove_bak() {
		new File(filename + ".bak").deleteOnExit();
	}

}
