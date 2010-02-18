package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;

import suneido.database.DbCheck.Status;

public class DbCheckRebuildBase extends TestBaseBase {
	protected final String filename;

	protected DbCheckRebuildBase(String filename) {
		this.filename = filename;
	}

	protected void dbcheck() {
		DbCheck dbck = new DbCheck(filename);
		Status status = dbck.checkPrint();
		assertEquals(Status.OK, status);
	}

	protected void checkTable() {
		checkTable(4);
	}
	protected void checkTable(int n) {
		int[] values = new int[n];
		for (int i = 0; i < n; ++i)
			values[i] = i;
		db = new Database(filename, Mode.OPEN);
		try {
			check("mytable", values);
		} finally {
			db.close();
			db = null;
		}
	}

	protected void checkNoTable() {
		db = new Database(filename, Mode.OPEN);
		try {
			checkNoTable("mytable");
		} finally {
			db.close();
			db = null;
		}
	}

	protected void checkNoTable(String tablename) {
		Transaction t = db.readonlyTran();
		assertNull(t.getTable(tablename));
		t.ck_complete();
	}

	protected void closeDb() {
		db.close();
		db = null;
	}

	@After
	public void delete() {
		new File(filename).delete();
	}

}
