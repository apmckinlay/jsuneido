package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Test;

public class DbRebuildTest extends DbCheckRebuildTestBase {

	@Test
	public void test_empty() {
		Database.create(filename).close();
		dbrebuild();
		dbcheckout();
	}

	@Test
	public void test_empty_table() {
		db = Database.create(filename);
		try {
			makeTable("mytable", 0);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheckout();
		checkTable(0);
	}

	@Test
	public void test_simple() {
		db = Database.create(filename);
		try {
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheckout();
		checkTable();
	}

	@Test
	public void test_add_index_with_existing_records() {
		db = Database.create(filename);
		try {
			makeTable("mytable", 4);
			db.addIndex("mytable", "b", false);
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheckout();
		checkTable();
	}

	@Test
	public void test_drop() {
		db = Database.create(filename);
		try {
			makeTable("mytable", 4);
			db.dropTable("mytable");
		} finally {
			db.close();
		}
		checkNoTable();
		dbrebuild();
		dbcheckout();
		checkNoTable();
	}

	@Test
	public void test_drop_recreate() {
		db = Database.create(filename);
		try {
			makeTable("mytable", 4);
			db.dropTable("mytable");
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		checkTable();
		dbrebuild();
		dbcheckout();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		db = Database.create(filename);
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
		dbcheckout();
		checkTable(8);
	}

	@Test
	public void test_views() {
		db = Database.create(filename);
		try {
			db.addView("myview", "indexes");
		} finally {
			db.close();
		}
		dbrebuild();
		dbcheckout();
		db = Database.openReadonly(filename);
		try {
			Transaction t = db.readTransaction();
			assertEquals("indexes", db.getView(t, "myview"));
		} finally {
			db.close();
		}
	}

	private void dbrebuild() {
		String result = DbRebuild.rebuild(filename, outfilename);
		assertTrue(result != null);
	}

	@After
	public void remove_bak() {
		new File(filename + ".bak").deleteOnExit();
	}

}
