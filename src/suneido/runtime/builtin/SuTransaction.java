/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.Trace.trace;
import static suneido.Trace.tracing;
import static suneido.Trace.Type.QUERY;
import static suneido.runtime.FunctionSpec.NA;
import static suneido.util.Util.array;

import java.util.Map;

import suneido.*;
import suneido.database.query.CompileQuery;
import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.DbmsTran;
import suneido.runtime.*;

public class SuTransaction extends SuValue {
	private final DbmsTran t;
	private boolean update = false;
	private String conflict = null;
	private SuObject data = null;
	private static final BuiltinMethods methods =
			new BuiltinMethods("transaction", SuTransaction.class, "Transactions");

	private static final FunctionSpec tranFS =
			new FunctionSpec(array("read", "update"), NA, NA);

	public SuTransaction(DbmsTran t) {
		assert t != null;
		this.t = t;
		update = !t.isReadonly();
	}

	public SuTransaction(Object[] args) {
		args = Args.massage(tranFS, args);
		if ((args[0] == NA) == (args[1] == NA))
			throw new SuException("usage: Transaction(read: [, block ]) "
					+ "or Transaction(update: [, block ])");
		if (args[0] == NA)
			update = Ops.toIntBool(args[1]) == 1;
		else
			update = Ops.toIntBool(args[0]) != 1;
		t = TheDbms.dbms().transaction(update);
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static Object Complete(Object self) {
		((SuTransaction) self).complete();
		return null;
	}

	private void complete() {
		checkNotEnded("Complete");
		conflict = t.complete();
		if (conflict != null)
			throw new SuException("transaction.Complete failed: " + conflict);
	}

	public static Object Conflict(Object self) {
		SuTransaction tran = (SuTransaction) self;
		return tran.conflict == null ? "" : tran.conflict;
	}

	public static Object EndedQ(Object self) {
		return ((SuTransaction) self).t.isEnded();
	}

	public static Object ReadCount(Object self) {
		return ((SuTransaction) self).t.readCount();
	}

	public static Object WriteCount(Object self) {
		return ((SuTransaction) self).t.writeCount();
	}

	private static final FunctionSpec QueryParams =
			ArgsArraySpec.from("query, block = false");

	public static Object Query(Object self, Object... args) {
		String where = queryWhere(args);
		args = Args.massage(QueryParams, args);
		String query = Ops.toStr(args[0]) + where;
		SuTransaction tran = (SuTransaction) self;
		tran.checkNotEnded("query");
		if (tracing(QUERY))
			trace(QUERY, tran + " " + query);
		if (CompileQuery.isRequest(query)) {
			if (args[1] != Boolean.FALSE)
				throw new SuException(
						"transaction.Query: block not allowed on request");
			return tran.t.request(query);
		}
		SuQuery q = new SuQuery(query, tran.t.query(query), tran.t);
		if (args[1] == Boolean.FALSE)
			return q;
		try {
			return Ops.call(args[1], q);
		} finally {
			if (!tran.t.isEnded())
				q.close();
		}
	}

	public static Object QueryFirst(Object self, Object... args) {
		return queryOne((SuTransaction) self, args, Dir.NEXT, false);
	}

	public static Object QueryLast(Object self, Object... args) {
		return queryOne((SuTransaction) self, args, Dir.PREV, false);
	}

	public static Object Query1(Object self, Object... args) {
		return queryOne((SuTransaction) self, args, Dir.NEXT, true);
	}

	static final FunctionSpec queryOneFS = new FunctionSpec("query");

	public static Object queryOne(SuTransaction ti, Object[] args, Dir dir,
			boolean single) {
		if (ti != null)
			ti.checkNotEnded("query");
		String where = queryWhere(args);
		args = Args.massage(queryOneFS, args);
		String query = Ops.toStr(args[0]) + where; //TODO insert where before sort
		return queryOne(ti, query, dir, single);
	}

	public static Object queryOne(SuTransaction ti, String query, Dir dir,
			boolean single) {
		if (tracing(QUERY))
			trace(QUERY, (ti == null ? "" : ti + " ") +
					(single ? "ONE" : dir == Dir.NEXT ? "FIRST" : "LAST") +
					" " + query);
		HeaderAndRow hr = (ti == null)
				? TheDbms.dbms().get(dir, query, single)
				: ti.t.get(dir, query, single);
		return hr == null ? false : new SuRecord(hr.row, hr.header, ti);
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
			if (key.equals("block") && SuValue.isCallable(value))
				continue;
			where.append(" where ")
					.append(key).append(" = ").append(Ops.display(value));
		}
		return where.toString();
	}

	public static Object Rollback(Object self) {
		SuTransaction tran = (SuTransaction) self;
		tran.checkNotEnded("Rollback");
		tran.t.abort();
		return null;
	}

	public static Object UpdateQ(Object self) {
		return ((SuTransaction) self).update;
	}

	public static Object Data(Object self) {
		SuTransaction tran = (SuTransaction) self;
		if (tran.data == null)
			tran.data = new SuObject();
		return tran.data;
	}

	// used by Transaction for block form
	private void block_complete() {
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

	public DbmsTran getTransaction() {
		return t;
	}

	public boolean isEnded() {
		return t.isEnded();
	}

	private void checkNotEnded(String op) {
		if (isEnded())
			throw new SuException("can't " + op + " ended transaction" +
					(conflict == null ? "" : " (" + conflict + ")"));
	}

	@Override
	public String toString() {
		return t.toString();
	}

	@Override
	public String typeName() {
		return "Transaction";
	}

	public static final BuiltinClass clazz = new BuiltinClass("Transaction",
			tranFS) {

		@Override
		public SuTransaction newInstance(Object... args) {
			return new SuTransaction(args);
		}

		FunctionSpec callFS = new FunctionSpec(
				array("read", "update", "block"), false, false, false);

		@Override
		public Object call(Object... args) {
			SuTransaction t = newInstance(args);
			args = Args.massage(callFS, args);
			if (args[2] == Boolean.FALSE)
				return t;
			try {
				Object result = Ops.call1(args[2], t);
				t.block_complete();
				return result;
			} catch (BlockReturnException bre) {
				t.block_complete();
				throw bre;
			} finally {
				t.abort();
			}
		}
	};

}
