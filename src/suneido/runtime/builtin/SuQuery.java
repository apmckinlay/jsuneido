/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.SuObject;
import suneido.SuRecord;
import suneido.SuValue;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.DbmsQuery;
import suneido.database.server.DbmsTran;
import suneido.runtime.Args;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class SuQuery extends SuValue {
	protected String query;
	protected DbmsQuery q;
	private final DbmsTran t;
	private Dir eof = null;

	private static BuiltinMethods methods = new BuiltinMethods("query", SuQuery.class);

	public SuQuery(String query, DbmsQuery q, DbmsTran t) {
		this.query = query;
		this.q = q;
		this.t = t;
		assert t != null;
	}

	protected SuQuery(String query, DbmsQuery q) { // used by CursorInstance
		this.query = query;
		this.q = q;
		t = null;
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	@Override
	public String toString() {
		return "Query(" + Ops.display(query) + ")";
	}

	public static Object Close(Object self) {
		((SuQuery) self).q.close();
		return null;
	}

	public static Object Columns(Object self) {
		List<String> cols = new ArrayList<>();
		for (String col : ((SuQuery) self).q.header().columns())
			if (!col.endsWith("_deps"))
				cols.add(col);
		return new SuObject(cols);
	}

	public static Object RuleColumns(Object self) {
		return new SuObject(((SuQuery) self).q.header().rules());
	}

	public static Object Strategy(Object self) {
		return ((SuQuery) self).q.strategy();
	}

	public static Object Keys(Object self) {
		SuObject c = new SuObject();
		for (List<String> key : ((SuQuery) self).q.keys())
			c.add(listToCommas(key));
		return c;
	}

	public static Object NewRecord(Object self, Object... args) {
		return Args.collectArgs(new SuRecord(), args);
	}

	public static Object Next(Object self) {
		return ((SuQuery) self).getrec(Dir.NEXT);
	}

	public static Object Prev(Object self) {
		return ((SuQuery) self).getrec(Dir.PREV);
	}

	private Object getrec(Dir dir) {
		return getrec(dir, t);
	}

	protected Object getrec(Dir dir, DbmsTran t) {
		if (eof == dir)
			return Boolean.FALSE;
		if (t.isEnded())
			throw new SuException("can't use ended Transaction");
		Row row = q.get(dir);
		eof = (row == null) ? dir : null;
		return (row == null) ? Boolean.FALSE : new SuRecord(row, q.header(), t);
	}

	@Params("record")
	public static Object Output(Object self, Object a) {
		SuObject rec = Ops.toObject(a);
		if (rec == null)
			throw new SuException("can't convert " + Ops.typeName(a) + " to object");
		DbmsQuery q = ((SuQuery) self).q;
		q.output(rec.toDbRecord(q.header()));
		return null;
	}

	public static Object Order(Object self) {
		return new SuObject(((SuQuery) self).q.ordering());
	}

	public static Object Rewind(Object self) {
		SuQuery query = (SuQuery) self;
		query.q.rewind();
		query.eof = null;
		return null;
	}

	@Override
	public String typeName() {
		return "Query";
	}

	public void close() {
		q.close();
	}

}
