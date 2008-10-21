package suneido.database.server;

import static suneido.Suneido.verify;

import java.util.*;

public class ServerData {
	private int next = 0;
	private final Map<Integer, DbmsTran> trans = new HashMap<Integer, DbmsTran>();
	private final Map<Integer, List<Integer>> tranqueries = new HashMap<Integer, List<Integer>>();
	private final Map<Integer, DbmsQuery> queries = new HashMap<Integer, DbmsQuery>();
	private final Map<Integer, DbmsQuery> cursors = new HashMap<Integer, DbmsQuery>();

	public int addTransaction(DbmsTran tran) {
		trans.put(next, tran);
		tranqueries.put(next, new ArrayList<Integer>());
		return next++;
	}

	public void endTransaction(int tn) {
		for (Integer qn : tranqueries.get(tn))
			queries.remove(qn);
		verify(trans.remove(tn) != null);
		if (trans.isEmpty())
			verify(queries.isEmpty());
	}

	public int addQuery(int tn, DbmsQuery q) {
		queries.put(next, q);
		tranqueries.get(tn).add(next);
		return next++;
	}

	public void endQuery(int qn) {
		verify(queries.remove(qn) != null);
	}

	public int addCursor(DbmsQuery q) {
		verify(q != null);
		cursors.put(next, q);
		return next++;
	}

	public void endCursor(int qn) {
		verify(cursors.remove(qn) != null);
	}

	public DbmsTran getTransaction(int tn) {
		return trans.get(tn);
	}

	public DbmsQuery getQuery(int qn) {
		return queries.get(qn);
	}

	public DbmsQuery getCursor(int cn) {
		return cursors.get(cn);
	}

	public boolean isEmpty() {
		return trans.isEmpty();
	}
}
