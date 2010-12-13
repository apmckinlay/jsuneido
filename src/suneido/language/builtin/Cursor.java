package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.*;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.language.*;

public class Cursor extends BuiltinClass {

	@Override
	public Instance newInstance(Object[] args) {
		return new Instance(args);
	}

	private static final FunctionSpec fs =
			new FunctionSpec(array("query", "block"), Boolean.FALSE);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		Instance f = newInstance(array(args[0]));
		if (args[1] == Boolean.FALSE)
			return f;
		else {
			Object result = Ops.call(args[1], f);
			f.Close();
			return result;
		}
	}

	private static class Instance extends QueryInstance {

		private static final FunctionSpec newFS = new FunctionSpec("query");

		Instance(Object[] args) {
			args = Args.massage(newFS, args);
			query = Ops.toStr(args[0]);
			q = TheDbms.dbms().cursor(query);
		}

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Next")
				return getrec(args, Dir.NEXT);
			if (method == "Output")
				throw new SuException("cursor.Output not implemented yet"); // TODO cursor.Output
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
			q.setTransaction(t.getTransaction());
			Row row = q.get(dir);
			return row == null ? Boolean.FALSE : new SuRecord(row, q.header(), t);
		}

		@Override
		public String toString() {
			return "Cursor(" + Ops.display(query) + ")";
		}

	}
}
