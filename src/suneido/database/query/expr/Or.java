package suneido.database.query.expr;

public class Or extends Multi {

	@Override
	public String toString() {
		return super.toString(" or ");
	}

	@Override
	public Expr fold() {
		return fold_exprs(Constant.FALSE, Constant.TRUE);
	}

}
