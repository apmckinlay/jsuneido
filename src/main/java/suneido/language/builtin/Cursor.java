/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.SuException;
import suneido.SuValue;
import suneido.TheDbms;
import suneido.database.query.Query.Dir;
import suneido.database.server.DbmsTran;
import suneido.language.*;

public class Cursor extends SuQuery {
	private static BuiltinMethods methods = new BuiltinMethods(Cursor.class);

	Cursor(String query) {
		super(query, TheDbms.dbms().cursor(query));
	}

	@Override
	public SuValue lookup(String method) {
		SuValue m = methods.getMethod(method);
		if (m != null)
			return m;
		return super.lookup(method);
	}

	@Params("transaction")
	public static Object Next(Object self, Object t) {
		return ((Cursor) self).getrec(t, Dir.NEXT);
	}

	@Params("transaction")
	public static Object Prev(Object self, Object t) {
		return ((Cursor) self).getrec(t, Dir.PREV);
	}

	public static Object Output(Object self, Object... args) {
		throw new SuException("cursor.Output not implemented yet"); // TODO cursor.Output
	}

	private Object getrec(Object arg, Dir dir) {
		if (!(arg instanceof SuTransaction))
			throw new SuException("usage: cursor.Next/Prev(transaction)");
		DbmsTran t = ((SuTransaction) arg).getTransaction();
		q.setTransaction(t);
		return super.getrec(dir, t);
	}

	@Override
	public String toString() {
		return "Cursor(" + Ops.display(query) + ")";
	}

	@Override
	public String typeName() {
		return "Cursor";
	}

	public static final SuValue clazz = new BuiltinClass() {

		FunctionSpec newFS = new FunctionSpec("query");
		@Override
		protected SuQuery newInstance(Object... args) {
			args = Args.massage(newFS, args);
			return new Cursor(Ops.toStr(args[0]));
		}

		FunctionSpec callFS = new FunctionSpec(array("query", "block"), false);
		@Override
		public Object call(Object... args) {
			args = Args.massage(callFS, args);
			SuQuery query = new Cursor(Ops.toStr(args[0]));
			if (args[1] == Boolean.FALSE)
				return query;
			else {
				try {
					return Ops.call(args[1], query);
				} finally {
					query.q.close();
				}
			}
		}
	};

}
