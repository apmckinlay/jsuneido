package suneido.database.query.expr;

import static suneido.Util.union;

import java.util.List;

import suneido.SuBoolean;
import suneido.SuValue;
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
	public SuValue eval(Header hdr, Row row) {
		if (expr.eval(hdr, row) == SuBoolean.TRUE)
			return iftrue.eval(hdr, row);
		else
			return iffalse.eval(hdr, row);
	}

}
