package suneido.database.query.expr;

import java.util.List;

public class And extends Multi {

	public And() {
		super();
	}

	public And(List<Expr> exprs) {
		super(exprs);
	}

	@Override
	public String toString() {
		return super.toString(" and ");
	}

	@Override
	public Expr fold() {
		return foldExprs(Constant.TRUE, Constant.FALSE);
	}

}
