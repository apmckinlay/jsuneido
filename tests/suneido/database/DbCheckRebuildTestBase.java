package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.Before;

import suneido.database.*;
import suneido.database.DbCheck.Status;
import suneido.util.FileUtils;

public class DbCheckRebuildTestBase extends TestBaseBase {
	protected String filename;
	protected String outfilename;

	@Before
	public void create() {
		File file = FileUtils.tempfile();
		filename = file.toString();
		outfilename = filename + ".out";
	}

	@After
    public void delete() {
    	new File(filename).deleteOnExit();
    	new File(outfilename).deleteOnExit();
    }

	protected void dbcheck() {
		assertEquals(Status.OK, DbCheck.check(outfilename));
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
			Transaction t = db.readonlyTran();
			TableData td = t.getTableData(t.getTable("mytable").num);
			assertEquals(2, td.nextfield);
			t.ck_complete();
		} finally {
			db.close();
		}
	}

	protected void checkNoTable() {
		db = new Database(filename, Mode.OPEN);
		try {
			checkNoTable("mytable");
		} finally {
			db.close();
		}
	}

	protected void checkNoTable(String tablename) {
		Transaction t = db.readonlyTran();
		assertNull(t.getTable(tablename));
		t.ck_complete();
	}

}
