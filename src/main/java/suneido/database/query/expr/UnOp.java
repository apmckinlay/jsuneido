/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import static suneido.SuInternalError.unreachable;

import java.util.List;

import suneido.compiler.Token;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.runtime.Ops;

public class UnOp extends Expr {
	private final Token op;
	private Expr expr;

	public UnOp(Token op, Expr expr) {
		this.op = op;
		this.expr = expr;
	}

	@Override
	public String toString() {
		return op.string + " " + expr;
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

	@Override
	public Object eval(Header hdr, Row row) {
		return eval2(expr.eval(hdr, row));
	}

	Object eval2(Object x) {
		switch (op) {
		case NOT:
			return Ops.not(x);
		case ADD:
			return x;
		case SUB:
			return Ops.uminus(x);
		case BITNOT:
			return Ops.bitnot(x);
		default:
			throw unreachable();
		}
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		Expr new_expr = expr.rename(from, to);
		return new_expr == expr ? this : new UnOp(op, new_expr);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		Expr new_expr = expr.replace(from, to);
		return new_expr == expr ? this : new UnOp(op, new_expr);
	}
}
