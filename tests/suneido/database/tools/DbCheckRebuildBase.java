package suneido.database.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.Before;

import suneido.database.*;
import suneido.database.tools.DbCheck.Status;

public class DbCheckRebuildBase extends TestBaseBase {
	protected String filename;
	protected String outfilename;

	@Before
	public void create() {
		File file = DbTools.tempfile();
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
		TheDb.open(filename, Mode.OPEN);
		try {
			check("mytable", values);
			Transaction t = TheDb.db().readonlyTran();
			TableData td = t.getTableData(t.getTable("mytable").num);
			assertEquals(2, td.nextfield);
			t.ck_complete();
		} finally {
			TheDb.close();
		}
	}

	protected void checkNoTable() {
		TheDb.open(filename, Mode.OPEN);
		try {
			checkNoTable("mytable");
		} finally {
			TheDb.close();
		}
	}

	protected void checkNoTable(String tablename) {
		Transaction t = TheDb.db().readonlyTran();
		assertNull(t.getTable(tablename));
		t.ck_complete();
	}

}
