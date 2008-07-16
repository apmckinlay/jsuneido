package suneido.database.query.expr;

import java.util.List;

public class TriOp extends Expr {
	private final Expr expr;
	private final Expr iftrue;
	private final Expr iffalse;

	public TriOp(Expr expr, Expr iftrue, Expr iffalse) {
		this.expr = expr;
		this.iftrue = iftrue;
		this.iffalse = iffalse;
	}

	@Override
	public String toString() {
		return "(" + expr + " ? " + iftrue + " : " + iffalse + ")";
	}

	@Override
	public List<String> fields() {
		// TODO Auto-generated method stub
		return null;
	}

}
