package suneido.database.query.expr;

import static suneido.SuException.unreachable;

import java.util.List;

import suneido.*;

public class UnOp extends Expr {
	private final Op op;
	private Expr expr;
	public enum Op {
		PLUS("+"), MINUS("-"), NOT("!"), BITNOT("~");
		public String name;
		Op(String name) {
			this.name = name;
		}
	}

	public UnOp(Op op, Expr expr) {
		this.op = op;
		this.expr = expr;
	}

	@Override
	public String toString() {
		return op.name + expr;
	}

	@Override
	public List<String> fields() {
		return expr.fields();
	}

	@Override
	public Expr fold() {
		expr = expr.fold();
		if (expr instanceof Constant)
			return Constant.valueOf(eval2(((Constant) expr).value));
		return this;
	}

	SuValue eval2(SuValue x) {
		switch (op) {
		case NOT:
			return x == SuBoolean.FALSE ? SuBoolean.TRUE : SuBoolean.FALSE;
		case BITNOT:
			return SuInteger.valueOf(~x.integer());
		case PLUS:
			return x;
		case MINUS:
			return x.uminus();
		default:
			throw unreachable();
		}
	}
}
