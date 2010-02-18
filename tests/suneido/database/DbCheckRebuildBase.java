package suneido.database;

import static org.junit.Assert.assertEquals;

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
		db = new Database(filename, Mode.OPEN);
		try {
			check("mytable", new int[] { 0, 1, 2, 3 });
		} finally {
			db.close();
			db = null;
		}
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
