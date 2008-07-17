package suneido.database.query.expr;

import java.util.List;

import suneido.SuBoolean;
import suneido.SuException;
import suneido.SuInteger;
import suneido.SuValue;

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

	@Override
	public Expr fold() {
		Expr new_expr = expr.fold();
		if (new_expr instanceof Constant)
			return new Constant(eval2(((Constant) new_expr).value));
		return new_expr == expr ? this : new UnOp(op, new_expr);
	}

	SuValue eval2(SuValue x) {
		switch (op.charAt(0)) {
		case '!':
		case 'n':
			return x == SuBoolean.FALSE ? SuBoolean.TRUE : SuBoolean.FALSE;
		case '~':
			return SuInteger.valueOf(~x.integer());
		case '+':
			return x;
		case '-':
			return x.uminus();
		default:
			throw new SuException("invalid UnOp type");
		}
	}
}
