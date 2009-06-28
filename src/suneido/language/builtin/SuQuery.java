package suneido.language.builtin;

import java.util.List;

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
		if (method == "Close")
			return null;
		if (method == "Columns")
			return columns(args);
		if (method == "Explain")
			return explain(args);
		if (method == "Fields") // deprecated
			return columns(args);
		if (method == "Next")
			return get(args, Dir.NEXT);
		if (method == "Output")
			return output(args);
		if (method == "Prev")
			return get(args, Dir.PREV);
		if (method == "Strategy")
			return explain(args);
		return super.invoke(self, method, args);
	}

	private Object columns(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		List<String> cols = q.header().columns();
		return new SuContainer(cols);
	}

	private String explain(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return q.toString();
	}

	private Object get(Object[] args, Dir dir) {
		Args.massage(FunctionSpec.noParams, args);
		Row row = q.get(dir);
		return row == null ? Boolean.FALSE : new SuRecord(row, q.header());
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
