package suneido.database.query;

import java.util.List;

import suneido.database.Transaction;
import suneido.database.query.expr.Expr;


public class Update extends QueryAction {
	private final List<String> fields;
	private final List<Expr> exprs;

	public Update(Query source, List<String> fields, List<Expr> exprs) {
		super(source);
		this.fields = fields;
		this.exprs = exprs;
	}

	@Override
	public String toString() {
		String s = "UPDATE " + source + " SET ";
		for (int i = 0; i < fields.size(); ++i)
			s += fields.get(i) + "=" + exprs.get(i) + ", ";
		return s.substring(0, s.length() - 2);
	}

	@Override
	int execute(Transaction tran) {
		// TODO Auto-generated method stub
		return 0;
	}

}
