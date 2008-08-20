package suneido.database.server;

import java.util.HashMap;
import java.util.Map;

import suneido.database.query.Query;

public class ServerData {
	private int next = 0;
	private final Map<Integer, DbmsTran> trans = new HashMap<Integer, DbmsTran>();
	private final Map<Integer, DbmsQuery> queries = new HashMap<Integer, DbmsQuery>();
	private final Map<Integer, DbmsQuery> cursors = new HashMap<Integer, DbmsQuery>();

	int addTransaction(DbmsTran tran) {
		int n = ++next;
		trans.put(n, tran);
		return n;
	}

	int addQuery(Query q) {
		int n = ++next;
		queries.put(n, q);
		return n;
	}

	int addCursor(Query q) {
		int n = ++next;
		cursors.put(n, q);
		return n;
	}

	DbmsTran getTransaction(int tn) {
		return trans.get(tn);
	}

	DbmsQuery getQuery(int qn) {
		return queries.get(qn);
	}

	DbmsQuery getCursor(int cn) {
		return cursors.get(cn);
	}
}
