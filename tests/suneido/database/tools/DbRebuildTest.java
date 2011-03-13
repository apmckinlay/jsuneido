package suneido.database.tools;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import suneido.database.*;
import suneido.database.tools.DbCheck.Status;


public class DbRebuildTest extends DbCheckRebuildTestBase {

	@Test
	public void test_empty() {
		TheDb.open(filename, Mode.CREATE);
		TheDb.close();
		dbrebuild();
		dbcheck();
	}

	@Test
	public void test_empty_table() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 0);
		} finally {
			TheDb.close();
		}
		dbrebuild();
		dbcheck();
		checkTable(0);
	}

	@Test
	public void test_simple() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			TheDb.close();
		}
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_add_index_with_existing_records() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			TheDb.db().addIndex("mytable", "b", false);
		} finally {
			TheDb.close();
		}
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_drop() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			TheDb.db().removeTable("mytable");
		} finally {
			TheDb.close();
		}
		checkNoTable();
		dbrebuild();
		dbcheck();
		checkNoTable();
	}

	@Test
	public void test_drop_recreate() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			TheDb.db().removeTable("mytable");
			makeTable("mytable", 4);
		} finally {
			TheDb.close();
		}
		checkTable();
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("tmp", 4);
			TheDb.db().removeTable("tmp"); // so new theDB() has different offsets
			makeTable("mytable_before", 4);
			TheDb.db().renameTable("mytable_before", "mytable");
			addRecords("mytable", 4, 7);
		} finally {
			TheDb.close();
		}
		dbrebuild();
		dbcheck();
		checkTable(8);
	}

	@Test
	public void test_views() {
		TheDb.open(filename, Mode.CREATE);
		try {
			TheDb.db().add_view("myview", "indexes");
		} finally {
			TheDb.close();
		}
		dbrebuild();
		dbcheck();
		TheDb.open(filename, Mode.OPEN);
		try {
			Transaction t = TheDb.db().readonlyTran();
			assertEquals("indexes", Database.getView(t, "myview"));
		} finally {
			TheDb.close();
		}
	}

	private void dbrebuild() {
		DbRebuild dbr = new DbRebuild(filename, outfilename, false);
		Status status = dbr.check();
		assertEquals(Status.OK, status);
		dbr.rebuild();
	}

	@After
	public void remove_bak() {
		new File(filename + ".bak").deleteOnExit();
	}

}
