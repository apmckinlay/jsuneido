package suneido.language.builtin;

import suneido.*;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;
import suneido.database.server.DbmsQuery;
import suneido.language.*;

public class SuQuery extends SuValue {
	private final DbmsQuery q;

	public SuQuery(DbmsQuery q) {
		this.q = q;
	}

	@Override
	public String toString() {
		return "query";
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Next")
			return get(args, Dir.NEXT);
		if (method == "Output")
			return output(args);
		if (method == "Prev")
			return get(args, Dir.PREV);
		return super.invoke(self, method, args);
	}

	private Object get(Object[] args, Dir dir) {
		Args.massage(FunctionSpec.noParams, args);
		Row row = q.get(dir);
		return row == null ? false : new SuRecord(row, q.header(), null);
	}

	private static final FunctionSpec recFS = new FunctionSpec("record");

	private Object output(Object[] args) {
		args = Args.massage(recFS, args);
		if (!(args[0] instanceof SuContainer))
			throw new SuException("can't convert " + Ops.typeName(args[0]) + " to object");
		SuContainer rec = (SuContainer) args[0];
		q.output(rec.toDbRecord(q.header()));
		return null;
	}

}
