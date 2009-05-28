package suneido.database.server;

import static suneido.Suneido.verify;

import java.util.*;

import suneido.database.Transaction;
import suneido.database.query.Query;

/**
 * Each connection/session has it's own ServerData instance
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class ServerData {
	private int next = 0;
	private final Map<Integer, DbmsTran> trans = new HashMap<Integer, DbmsTran>();
	private final Map<Integer, List<Integer>> tranqueries = new HashMap<Integer, List<Integer>>();
	private final Map<Integer, Query> queries = new HashMap<Integer, Query>();
	private final Map<Integer, Query> cursors = new HashMap<Integer, Query>();
	private final Map<String, String> sviews = new HashMap<String, String>();
	private final Stack<String> viewnest = new Stack<String>();

	/**
	 * this is set by {@link Server} since it is per connection, not really per
	 * thread
	 */
	public static final ThreadLocal<ServerData> threadLocal =
			new ThreadLocal<ServerData>();
	public static ServerData forThread() {
		return threadLocal.get();
	}

	public int addTransaction(DbmsTran tran) {
		// client expect readonly tran# to be even
		if (((Transaction) tran).isReadonly() && (next % 2) != 0)
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

	public void addSview(String name, String definition) {
		sviews.put(name, definition);
	}
	public String getSview(String name) {
		return sviews.get(name);
	}
	public void dropSview(String name) {
		sviews.remove(name);
	}

	public void enterView(String name) {
		viewnest.push(name);
	}
	public boolean inView(String name) {
		return -1 != viewnest.search(name);
	}
	public void leaveView(String name) {
		assert viewnest.peek().equals(name);
		viewnest.pop();
	}

	public int cursorsSize() {
		return cursors.size();
	}
}
