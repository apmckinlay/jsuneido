package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.Suneido.dbpkg;
import static suneido.database.query.Query.Dir.PREV;

import org.junit.Test;

import suneido.database.server.ServerData;
import suneido.intfc.database.Database;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class ProjectTest {
	private final ServerData serverData = new ServerData();
	private final Database db = dbpkg.testdb();

	@Test
	public void test() {
		try {
			Request.execute(db, "create tmp (a,b) key(a)");
			req("insert { a: 1 } into tmp");
			req("insert { a: 2, b: 1 } into tmp");
			Transaction t = db.readonlyTran();
			try {
				Query q = CompileQuery.query(t, serverData, "tmp project b");
				Record r;
				r = q.get(PREV).firstData();
				assertEquals(1, r.get(1));
				r = q.get(PREV).firstData();
				assertEquals("", r.get(1));
				assertEquals(null, q.get(PREV));
				t.complete();
			} finally {
				t.abortIfNotComplete();
			}
		} finally {
			db.close();
		}
	}

	protected int req(String s) {
		Transaction tran = db.readwriteTran();
		try {
			Query q = CompileQuery.parse(tran, serverData, s);
			int n = ((QueryAction) q).execute();
			tran.ck_complete();
			return n;
		} finally {
			tran.abortIfNotComplete();
		}
	}

}
