/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import java.util.ArrayList;
import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Ops;

public class And extends Multi {

	public And() {
	}

	public And(List<Expr> exprs) {
		super(exprs);
	}

	public static Object make(Expr expr1, Expr expr2) {
		if (expr1 instanceof And) {
			And and = (And) expr1;
			and.exprs.add(expr2);
			return and;
		} else {
			List<Expr> exprs = new ArrayList<>();
			exprs.add(expr1);
			exprs.add(expr2);
			return new And(exprs);
		}
	}

	@Override
	public String toString() {
		return super.toString(" and ");
	}

	@Override
	public Expr fold() {
		List<Expr> new_exprs = foldExprs(Constant.TRUE);
		boolean alltrue = true;
		for (Expr e : new_exprs == null ? exprs : new_exprs)
			if (e instanceof Constant &&
					! Ops.toBoolean_(((Constant) e).value))
				return Constant.FALSE;
			else
				alltrue = false;
		if (alltrue)
			return Constant.TRUE;
		return new_exprs == null ? this : new And(new_exprs);
	}

	@Override
	public Object eval(Header hdr, Row row) {
		for (Expr e : exprs)
			if (! Ops.toBoolean_(e.eval(hdr, row)))
				return Boolean.FALSE;
		return Boolean.TRUE;
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		List<Expr> new_exprs = renameExprs(from, to);
		return new_exprs == null ? this : new And(new_exprs);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		List<Expr> new_exprs = replaceExprs(from, to);
		return new_exprs == null ? this : new And(new_exprs);
	}

}
