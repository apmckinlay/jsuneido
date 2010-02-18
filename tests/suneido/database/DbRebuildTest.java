package suneido.database;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import suneido.database.DbCheck.Status;


public class DbRebuildTest extends DbCheckRebuildBase {

	public DbRebuildTest() {
		super("dbrebuildtest.db");
	}

	@Test
	public void test_empty() {
		db = new Database(filename, Mode.CREATE);
		closeDb();
		dbrebuild();
		dbcheck();
	}

	@Test
	public void test_simple() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			closeDb();
		}
DbDump.dump(filename);
		dbrebuild();
		dbcheck();
		checkTable();
DbDump.dump(filename);
	}

	@Test
	public void test_add_index_with_existing_records() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			db.addIndex("mytable", "b", false);
		} finally {
			closeDb();
		}
DbDump.dump(filename);
		dbrebuild();
		dbcheck();
		checkTable();
	}

	@Test
	public void test_drop() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			db.removeTable("mytable");
		} finally {
			closeDb();
		}
		dbrebuild();
		dbcheck();
	}

	@Test
	public void test_drop_recreate() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
			db.removeTable("mytable");
			makeTable("mytable", 4);
		} finally {
			closeDb();
		}
		dbrebuild();
		dbcheck();
		checkTable();
	}

	private void dbrebuild() {
		DbRebuild dbr = new DbRebuild(filename);
		Status status = dbr.checkPrint();
		assertEquals(Status.OK, status);
		dbr.rebuild();
	}

	@After
	public void remove_bak() {
		new File(filename + ".bak").delete();
	}

}
