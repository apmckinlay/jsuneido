package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;
import suneido.SuException;
import suneido.SuRecord;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.language.*;

public class CursorInstance extends QueryInstance {

	private static final FunctionSpec newFS = new FunctionSpec("query");

	CursorInstance(Object[] args) {
		args = Args.massage(newFS, args);
		query = Ops.toStr(args[0]);
		q = theDbms.cursor(ServerData.forThread(), query);
	}

	@Override
	public String toString() {
		return "Cursor(" + Ops.display(query) + ")";
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Next")
			return getrec(args, Dir.NEXT);
		if (method == "Output")
			throw new SuException("cursor.Output not implemented yet");
		if (method == "Prev")
			return getrec(args, Dir.PREV);
		return super.invoke(self, method, args);
	}

	private static final FunctionSpec getFS = new FunctionSpec("transaction");

	private Object getrec(Object[] args, Dir dir) {
		args = Args.massage(getFS, args);
		if (!(args[0] instanceof TransactionInstance))
			throw new SuException("usage: cursor.Next/Prev(transaction)");
		TransactionInstance t = (TransactionInstance) args[0];
		q.setTransaction((suneido.database.Transaction) t.getTransaction());
		try {
			Row row = q.get(dir);
			return row == null ? Boolean.FALSE : new SuRecord(row, q.header(), t);
		} finally {
			q.setTransaction(null);
		}
	}

}
