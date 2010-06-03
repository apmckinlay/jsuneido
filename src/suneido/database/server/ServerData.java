package suneido.database.server;

import static suneido.SuException.verify;

import java.util.*;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.Transaction;
import suneido.util.NetworkOutput;

/**
 * Each connection/session has it's own ServerData instance
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
@NotThreadSafe
public class ServerData {
	private int next = 0;
	private final Map<Integer, DbmsTran> trans =
			new HashMap<Integer, DbmsTran>();
	private final Map<Integer, List<Integer>> tranqueries =
			new HashMap<Integer, List<Integer>>();
	private final Map<Integer, DbmsQuery> queries =
			new HashMap<Integer, DbmsQuery>();
	private final Map<Integer, DbmsQuery> cursors =
			new HashMap<Integer, DbmsQuery>();
	private final Map<String, String> sviews = new HashMap<String, String>();
	private final Stack<String> viewnest = new Stack<String>();
	private String sessionId = "127.0.0.1";
	public final NetworkOutput outputQueue; // for kill
	public boolean textmode = true;

	/** for tests */
	public ServerData() {
		this.outputQueue = null;
	}

	public ServerData(NetworkOutput outputQueue) {
		this.outputQueue = outputQueue;
	}

	/**
	 * this is set by {@link DbmsServerBySelect} since it is per connection,
	 * not really per thread, initialValue is for tests
	 */
	public static final ThreadLocal<ServerData> threadLocal
		= new ThreadLocal<ServerData>() {
				@Override
				public ServerData initialValue() {
					return new ServerData();
				}
			};
	public static ServerData forThread() {
		return threadLocal.get();
	}

	public int addTransaction(DbmsTran tran) {
		int num = ((Transaction) tran).num;
		trans.put(num, tran);
		tranqueries.put(num, new ArrayList<Integer>());
		return num;
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

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void end() {
		for (Map.Entry<Integer, DbmsTran> e : trans.entrySet())
			e.getValue().abort();
	}
}
