package suneido.database.server;

import static suneido.Suneido.verify;

import java.util.*;

import suneido.database.Transaction;
import suneido.database.query.Query;

public class ServerData {
	private int next = 0;
	private final Map<Integer, DbmsTran> trans = new HashMap<Integer, DbmsTran>();
	private final Map<Integer, List<Integer>> tranqueries = new HashMap<Integer, List<Integer>>();
	private final Map<Integer, Query> queries = new HashMap<Integer, Query>();
	private final Map<Integer, Query> cursors = new HashMap<Integer, Query>();

	public int addTransaction(DbmsTran tran) {
		// client expect readonly tran# to be even
		if (((Transaction) tran).isReadonly() && (next % 2) == 1)
			++next;
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

	public int addQuery(int tn, Query q) {
		queries.put(next, q);
		tranqueries.get(tn).add(next);
		return next++;
	}

	public void endQuery(int qn) {
		verify(queries.remove(qn) != null);
	}

	public int addCursor(Query q) {
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

	public Query getQuery(int qn) {
		return queries.get(qn);
	}

	public Query getCursor(int cn) {
		return cursors.get(cn);
	}

	public boolean isEmpty() {
		return trans.isEmpty();
	}
}
