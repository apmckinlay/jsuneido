package suneido.language.builtin;

import static suneido.util.Util.listToCommas;

import java.util.List;

import suneido.*;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;
import suneido.database.server.DbmsQuery;
import suneido.database.server.DbmsTran;
import suneido.language.*;

public class QueryInstance extends SuValue {
	protected String query;
	protected DbmsQuery q;
	protected final DbmsTran t;
	protected Dir eof = null;


	public QueryInstance(String query, DbmsQuery q, DbmsTran t) {
		this.query = query;
		this.q = q;
		this.t = t;
		assert t != null;
	}

	protected QueryInstance() { // used by CursorInstance
		t = null;
	}

	@Override
	public String toString() {
		return "Query(" + Ops.display(query) + ")";
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Close")
			return null;
		if (method == "Columns")
			return Columns(args);
		if (method == "Explain")
			return Explain(args);
		if (method == "Fields") // deprecated - use Columns
			return Columns(args);
		if (method == "Keys")
			return Keys(args);
		if (method == "NewRecord")
			return NewRecord(args);
		if (method == "Next")
			return getrec(args, Dir.NEXT);
		if (method == "Order")
			return Order(args);
		if (method == "Output")
			return Output(args);
		if (method == "Prev")
			return getrec(args, Dir.PREV);
		if (method == "Rewind")
			return Rewind(args);
		if (method == "Strategy")
			return Explain(args);
		return super.invoke(self, method, args);
	}

	private Object Columns(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		List<String> cols = q.header().columns();
		return new SuContainer(cols);
	}

	private String Explain(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return q.toString();
	}

	private Object Keys(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		SuContainer c = new SuContainer();
		for (List<String> key : q.keys())
			c.append(listToCommas(key));
		return c;
	}

	private Object NewRecord(Object[] args) {
		return Args.collectArgs(args, new SuRecord());
	}

	private Object getrec(Object[] args, Dir dir) {
		Args.massage(FunctionSpec.noParams, args);
		if (eof == dir)
			return Boolean.FALSE;
		Row row = q.get(dir);
		eof = row == null ? dir : null;
		return row == null ? Boolean.FALSE : new SuRecord(row, q.header(), t);
	}

	private static final FunctionSpec recFS = new FunctionSpec("record");

	private Object Output(Object[] args) {
		args = Args.massage(recFS, args);
		SuContainer rec = Ops.toContainer(args[0]);
		if (rec == null)
			throw new SuException("can't convert " + Ops.typeName(args[0]) + " to object");
		q.output(rec.toDbRecord(q.header()));
		return Boolean.TRUE;
	}

	private Object Order(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuContainer(q.ordering());
	}

	private Object Rewind(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		q.rewind();
		eof = null;
		return null;
	}

}
