/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.List;

import suneido.*;
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

	private static BuiltinMethods methods = new BuiltinMethods(SuQuery.class);

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

	public static class Close extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			((SuQuery) self).q.close();
			return null;
		}
	}

	public static class Columns extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return columns(self);
		}
	}

	public static class Fields extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return columns(self);
		}
	}

	private static Object columns(Object self) {
		List<String> cols = new ArrayList<String>();
		for (String col : ((SuQuery) self).q.header().columns())
			if (!col.endsWith("_deps"))
				cols.add(col);
		return new SuContainer(cols);
	}

	public static class Explain extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return ((SuQuery) self).q.explain();
		}
	}

	public static class Strategy extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return ((SuQuery) self).q.explain();
		}
	}

	public static class Keys extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			SuContainer c = new SuContainer();
			for (List<String> key : ((SuQuery) self).q.keys())
				c.append(listToCommas(key));
			return c;
		}
	}

	public static class NewRecord extends SuMethod {
		@Override
		public Object eval(Object self, Object... args) {
			return Args.collectArgs(new SuRecord(), args);
		}
	}

	public static class Next extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return ((SuQuery) self).getrec(Dir.NEXT);
		}
	}

	public static class Prev extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return ((SuQuery) self).getrec(Dir.PREV);
		}
	}

	private Object getrec(Dir dir) {
		if (eof == dir)
			return Boolean.FALSE;
		Row row = q.get(dir);
		eof = row == null ? dir : null;
		return row == null ? Boolean.FALSE : new SuRecord(row, q.header(), t);
	}

	public static class Output extends SuMethod1 {
		{ params = new FunctionSpec("record"); }
		@Override
		public Object eval1(Object self, Object a) {
			SuContainer rec = Ops.toContainer(a);
			if (rec == null)
				throw new SuException("can't convert " + Ops.typeName(a) + " to object");
			DbmsQuery q = ((SuQuery) self).q;
			q.output(rec.toDbRecord(q.header()));
			return Boolean.TRUE;
		}
	}

	public static class Order extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return new SuContainer(((SuQuery) self).q.ordering());
		}
	}

	public static class Rewind extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			SuQuery query = (SuQuery) self;
			query.q.rewind();
			query.eof = null;
			return null;
		}
	}

	@Override
	public String typeName() {
		return "Query";
	}

}
