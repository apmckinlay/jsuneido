package suneido.language;

import static suneido.database.server.Command.theDbms;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;
import suneido.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.DbmsTran;
import suneido.database.server.ServerData;
import suneido.database.server.Dbms.HeaderAndRow;

public class SuTransaction extends SuValue {
	private final DbmsTran t;
	private boolean ended = false;

	private static final Object notPassed = new Object();
	private static final FunctionSpec tranFS =
			new FunctionSpec(array("read", "update"), notPassed, notPassed);

	public SuTransaction(Object[] args) {
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
			return queryOne(t, args, Dir.NEXT, false);
		if (method == "QueryLast")
			return queryOne(t, args, Dir.PREV, false);
		if (method == "Query1")
			return queryOne(t, args, Dir.NEXT, true);
		if (method == "Rollback")
			return rollback(args);
		return userDefined("Transactions", method).invoke(self, method, args);
	}

	private static final FunctionSpec queryFS =
			new FunctionSpec(array("query", "block"), false);

	private Object query(Object[] args) {
		args = Args.massage(queryFS, args);
		String query = Ops.toStr(args[0]);
		SuQuery q = new SuQuery(theDbms.query(new ServerData(), t, query));
		if (args[1] == Boolean.FALSE)
			return q;
		try {
			return Ops.call(args[1], q);
		} catch (BlockReturnException bre) {
			return bre.returnValue;
		}
	}

	// TODO keyword query arguments
	private static final FunctionSpec queryOneFS = new FunctionSpec("query");

	public static Object queryOne(DbmsTran t, Object[] args, Dir dir,
			boolean single) {
		args = Args.massage(queryOneFS, args);
		String query = Ops.toStr(args[0]);
		// TODO serverdata ???
		HeaderAndRow hr = theDbms.get(new ServerData(), dir, query, single, t);
		return hr.row == null ? false : new SuRecord(hr.row, hr.header, null);
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
	public boolean equals(Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
