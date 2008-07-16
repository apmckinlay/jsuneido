package suneido.database.query.expr;

import java.util.List;

public class BinOp extends Expr {
	private final String op;
	private final Expr expr1;
	private final Expr expr2;

	public BinOp(String op, Expr expr1, Expr expr2) {
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}


	@Override
	public String toString() {
		return "(" + expr1 + " " + op + " " + expr2 + ")";
	}


	@Override
	public List<String> fields() {
		// TODO Auto-generated method stub
		return null;
	}

}
