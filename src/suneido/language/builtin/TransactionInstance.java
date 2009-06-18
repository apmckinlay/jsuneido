package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;
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
	private final DbmsTran t;
	private boolean ended = false;

	private static final Object notPassed = new Object();
	private static final FunctionSpec tranFS =
			new FunctionSpec(array("read", "update"), notPassed, notPassed);

	public TransactionInstance(DbmsTran tran) {
		this.t = tran;
	}

	public TransactionInstance(Object[] args) {
		args = Args.massage(tranFS, args);
		if ((args[0] == notPassed) == (args[1] == notPassed))
			throw new SuException("usage: Transaction(read: [, block ]) "
					+ "or Transaction(update: [, block ])");
		boolean update;
		if (args[0] == notPassed)
			update = Ops.toBool(args[1]) == 1;
		else
			update = !(Ops.toBool(args[0]) == 1);
		t = theDbms.transaction(update);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Complete")
			return complete(args);
		if (method == "Query")
			return query(args);
		if (method == "QueryFirst")
			return queryOne(this, args, Dir.NEXT, false);
		if (method == "QueryLast")
			return queryOne(this, args, Dir.PREV, false);
		if (method == "Query1")
			return queryOne(this, args, Dir.NEXT, true);
		if (method == "Rollback")
			return rollback(args);
		return userDefined("Transactions", method).invoke(self, method, args);
	}

	private static final FunctionSpec queryFS =
			new FunctionSpec(array("query", "block"), false);

	private Object query(Object[] args) {
		args = Args.massage(queryFS, args);
		String query = Ops.toStr(args[0]);
		Object q;
		if (CompileQuery.isRequest(query)) {
			q = theDbms.request(ServerData.forThread(), t, query);
		} else {
			q = new SuQuery(theDbms.query(new ServerData(), t, query));
		}
		if (args[1] == Boolean.FALSE)
			return q;
		return Ops.call(args[1], q);
	}

	// TODO keyword query arguments
	private static final FunctionSpec queryOneFS = new FunctionSpec("query");

	public static Object queryOne(TransactionInstance t, Object[] args, Dir dir,
			boolean single) {
		args = Args.massage(queryOneFS, args);
		String query = Ops.toStr(args[0]);
		// TODO serverdata ???
		HeaderAndRow hr = theDbms.get(new ServerData(), dir, query, single,
				t == null ? null : t.getTransaction());
		return hr.row == null ? false : new SuRecord(hr.row, hr.header, t);
	}

	private Object complete(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		ended = true;
		return t.complete() == null;
	}

	private Object rollback(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		ended = true;
		t.abort();
		return null;
	}

	public void ck_complete() {
		if (!ended) {
			ended = true;
			String s = t.complete();
			if (s != null)
				throw new SuException("transaction commit failed: " + s);
		}
	}

	public void abort() {
		if (!ended) {
			ended = true;
			t.abort();
		}
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public DbmsTran getTransaction() {
		return t;
	}

	public boolean isEnded() {
		return ended;
	}

}
