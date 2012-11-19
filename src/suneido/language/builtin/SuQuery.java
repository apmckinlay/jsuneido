/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.List;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuRecord;
import suneido.SuValue;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.DbmsQuery;
import suneido.database.server.DbmsTran;
import suneido.language.*;

public class SuQuery extends SuValue {
	protected String query;
	protected DbmsQuery q;
	protected final DbmsTran t;
	protected Dir eof = null;

	private static BuiltinMethods2 methods = new BuiltinMethods2(SuQuery.class);

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
		return columns(self);
	}

	public static Object Fields(Object self) {
		return columns(self);
	}

	private static Object columns(Object self) {
		List<String> cols = new ArrayList<String>();
		for (String col : ((SuQuery) self).q.header().columns())
			if (!col.endsWith("_deps"))
				cols.add(col);
		return new SuContainer(cols);
	}

	public static Object Explain(Object self) {
		return ((SuQuery) self).q.explain();
	}

	public static Object Strategy(Object self) {
		return ((SuQuery) self).q.explain();
	}

	public static Object Keys(Object self) {
		SuContainer c = new SuContainer();
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

	protected Object getrec(Dir dir) {
		return getrec(dir, t);
	}

	protected Object getrec(Dir dir, DbmsTran t) {
		if (eof == dir)
			return Boolean.FALSE;
		Row row = q.get(dir);
		eof = row == null ? dir : null;
		return row == null ? Boolean.FALSE : new SuRecord(row, q.header(), t);
	}

	@Params("record")
	public static Object Output(Object self, Object a) {
		SuContainer rec = Ops.toContainer(a);
		if (rec == null)
			throw new SuException("can't convert " + Ops.typeName(a) + " to object");
		DbmsQuery q = ((SuQuery) self).q;
		q.output(rec.toDbRecord(q.header()));
		return Boolean.TRUE;
	}

	public static Object Order(Object self) {
		return new SuContainer(((SuQuery) self).q.ordering());
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
