package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.util.Map;

import suneido.*;
import suneido.database.query.CompileQuery;
import suneido.database.query.Query.Dir;
import suneido.database.server.DbmsTran;
import suneido.database.server.ServerData;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.language.*;

/**
 * SuTransaction is for instances of transactions
 * the class is {@link Transaction}
 *
 * @author Andrew McKinlay
 */
public class TransactionInstance extends SuValue {
	private static int nextnum = 0;
	private final int num = ++nextnum;
	private final DbmsTran t;
	private boolean update = false;
	private String conflict = null;

	private static final Object notPassed = new Object();
	private static final FunctionSpec tranFS =
			new FunctionSpec(array("read", "update"), notPassed, notPassed);

	public TransactionInstance(DbmsTran t) {
		assert t != null;
		this.t = t;
		update = !t.isReadonly();
	}

	public TransactionInstance(Object[] args) {
		args = Args.massage(tranFS, args);
		if ((args[0] == notPassed) == (args[1] == notPassed))
			throw new SuException("usage: Transaction(read: [, block ]) "
					+ "or Transaction(update: [, block ])");
		if (args[0] == notPassed)
			update = Ops.toBool(args[1]) == 1;
		else
			update = !(Ops.toBool(args[0]) == 1);
		t = theDbms.transaction(update);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Complete")
			return Complete(args);
		if (method == "Conflict")
			return Confict(args);
		if (method == "Ended?")
			return EndedQ(args);
		if (method == "Query")
			return Query(args);
		if (method == "QueryFirst")
			return queryOne(this, args, Dir.NEXT, false);
		if (method == "QueryLast")
			return queryOne(this, args, Dir.PREV, false);
		if (method == "Query1")
			return queryOne(this, args, Dir.NEXT, true);
		if (method == "Rollback")
			return Rollback(args);
		if (method == "Update?")
			return UpdateQ(args);
		return userDefined("Transactions", self, method, args);
	}

	private boolean Complete(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		conflict = t.complete();
		return conflict == null;
	}

	private String Confict(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return conflict == null ? "" : conflict;
	}

	private boolean EndedQ(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return t.isEnded();
	}

	private static final FunctionSpec queryFS =
			new FunctionSpec(array("query", "block"), false);

	private Object Query(Object[] args) {
		String where = queryWhere(args);
		args = Args.massage(queryFS, args);
		String query = Ops.toStr(args[0]) + where;
		Object q;
		if (CompileQuery.isRequest(query))
			q = theDbms.request(ServerData.forThread(), t, query);
		else
			q = new QueryInstance(query,
					theDbms.query(ServerData.forThread(), t, query), t);
		if (args[1] == Boolean.FALSE)
			return q;
		return Ops.call(args[1], q);
	}

	private static final FunctionSpec queryOneFS = new FunctionSpec("query");

	public static Object queryOne(TransactionInstance t, Object[] args, Dir dir,
			boolean single) {
		String where = queryWhere(args);
		args = Args.massage(queryOneFS, args);
		String query = Ops.toStr(args[0]) + where;
		HeaderAndRow hr = theDbms.get(ServerData.forThread(), dir, query, single,
				t == null ? null : t.getTransaction());
		return hr.row == null ? false : new SuRecord(hr.row, hr.header, t);
	}

	@SuppressWarnings("unchecked")
	private static String queryWhere(Object[] args) {
		ArgsIterator iter = new ArgsIterator(args);
		StringBuilder where = new StringBuilder();
		while (iter.hasNext()) {
			Object arg = iter.next();
			if (!(arg instanceof Map.Entry))
				continue;
			Map.Entry<Object, Object> e = (Map.Entry<Object, Object>) arg;
			Object key = e.getKey();
			Object value = e.getValue();
			if (key.equals("query"))
				continue;
			if (key.equals("block")
					&& (value instanceof SuCallable || value instanceof SuBlock))
				continue;
			where.append(" where ")
					.append(key).append(" = ").append(Ops.display(value));
		}
		return where.toString();
	}

	private Object Rollback(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (t.isEnded())
			throw new SuException("cannot Rollback completed Transaction");
		t.abort();
		return null;
	}

	private Object UpdateQ(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return update;
	}

	// used by Transaction for block form
	public void block_complete() {
		if (!t.isEnded()) {
			conflict = t.complete();
			if (conflict != null)
				throw new SuException("Transaction: block commit failed: " + conflict);
		}
	}

	public void abort() {
		if (!t.isEnded())
			t.abort();
	}

	@Override
	public String toString() {
		return /*"Transaction" + num + " " +*/ t.toString();
	}

	public DbmsTran getTransaction() {
		return t;
	}

	public boolean isEnded() {
		return t.isEnded();
	}

	public String conflict() {
		return conflict;
	}

}
