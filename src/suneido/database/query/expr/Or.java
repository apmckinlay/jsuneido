package suneido.database.query.expr;

public class Or extends Multi {

	@Override
	public String toString() {
		return super.toString(" or ");
	}

	@Override
	public Expr fold() {
		return foldExprs(Constant.FALSE, Constant.TRUE);
	}

}
