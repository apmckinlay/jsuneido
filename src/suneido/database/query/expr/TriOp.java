package suneido.database.query.expr;

import static suneido.Util.union;

import java.util.List;

import suneido.SuBoolean;

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
		return union(expr.fields(), union(iftrue.fields(), iffalse.fields()));
	}

	@Override
	public Expr fold() {
		Expr new_expr = expr.fold();
		Expr new_iftrue = iftrue.fold();
		Expr new_iffalse = iffalse.fold();
		if (new_expr instanceof Constant) {
			Constant kexpr = (Constant) new_expr;
			return kexpr.value == SuBoolean.TRUE ? new_iftrue : new_iffalse;
			}
		return new_expr == expr && new_iftrue == iftrue && new_iffalse == iffalse
			? this : new TriOp(new_expr, new_iftrue, new_iffalse);
	}

}
