package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.database.query.Query.Dir.PREV;

import org.junit.Test;

import suneido.database.*;
import suneido.database.server.ServerData;

public class ProjectTest {
	protected final ServerData serverData = new ServerData();

	@Test
	public void test() {
		TheDb.set(new Database(new DestMem(), Mode.CREATE));
		try {
			Request.execute("create tmp (a,b) key(a)");
			req("insert { a: 1 } into tmp");
			req("insert { a: 2, b: 1 } into tmp");
			Transaction t = TheDb.db().readonlyTran();
			try {
				Query q = CompileQuery.query(t, serverData, "tmp project b");
				Record r;
				r = q.get(PREV).getFirstData();
				assertEquals(1, r.get(1));
				r = q.get(PREV).getFirstData();
				assertEquals("", r.get(1));
				assertEquals(null, q.get(PREV));
				t.complete();
			} finally {
				t.abortIfNotComplete();
			}
		} finally {
			TheDb.db().close();
		}
	}

	protected int req(String s) {
		Transaction tran = TheDb.db().readwriteTran();
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
