package suneido.database.query;

import java.util.List;

import suneido.database.Transaction;
import suneido.database.query.expr.Expr;

public class Insert extends QueryAction {
	private final List<String> fields;
	private final List<Expr> exprs;

	public Insert(Query source, List<String> fields, List<Expr> exprs) {
		super(source);
		this.fields = fields;
		this.exprs = exprs;
	}

	@Override
	public String toString() {
		String s = "INSERT { ";
		for (int i = 0; i < fields.size(); ++i)
			s += fields.get(i) + ": " + exprs.get(i) + ", ";
		return s.substring(0, s.length() - 2) + " } INTO " + source;
	}

	@Override
	int execute(Transaction tran) {
		// TODO Auto-generated method stub
		return 1;
	}

}
