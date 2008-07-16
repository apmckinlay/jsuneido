package suneido.database.query.expr;

import java.util.List;

public class UnOp extends Expr {
	private final String op;
	private final Expr expr;

	public UnOp(String op, Expr expr) {
		this.op = op;
		this.expr = expr;
	}

	@Override
	public String toString() {
		return op + (op.equals("not") ? " " : "") + expr;
	}

	@Override
	public List<String> fields() {
		// TODO Auto-generated method stub
		return null;
	}

}
