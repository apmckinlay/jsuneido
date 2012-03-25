package suneido.database.query.expr;

import static suneido.util.Util.union;

import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;

public class TriOp extends Expr {
	private Expr expr;
	private Expr iftrue;
	private Expr iffalse;

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
		expr = expr.fold();
		iftrue = iftrue.fold();
		iffalse = iffalse.fold();
		if (expr instanceof Constant)
			return expr == Constant.TRUE ? iftrue : iffalse;
		return this;
	}

	@Override
	public Object eval(Header hdr, Row row) {
		if (expr.eval(hdr, row) == Boolean.TRUE)
			return iftrue.eval(hdr, row);
		else
			return iffalse.eval(hdr, row);
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		Expr new_expr = expr.rename(from, to);
		Expr new_iftrue = iftrue.rename(from, to);
		Expr new_iffalse = iffalse.rename(from, to);
		return new_expr == expr && new_iftrue == iftrue && new_iffalse == iffalse
			? this : new TriOp(new_expr, new_iftrue, new_iffalse);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		Expr new_expr = expr.replace(from, to);
		Expr new_iftrue = iftrue.replace(from, to);
		Expr new_iffalse = iffalse.replace(from, to);
		return new_expr == expr && new_iftrue == iftrue
				&& new_iffalse == iffalse ? this : new TriOp(new_expr,
				new_iftrue, new_iffalse);
	}
}
