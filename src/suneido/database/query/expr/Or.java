/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import java.util.ArrayList;
import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.runtime.Ops;

public class Or extends Multi {

	public Or() {
	}

	public Or(List<Expr> exprs) {
		super(exprs);
	}

	@Override
	public String toString() {
		return super.toString(" or ");
	}

	public static Object make(Expr expr1, Expr expr2) {
		if (expr1 instanceof Or) {
			Or or = (Or) expr1;
			or.exprs.add(expr2);
			return or;
		} else {
			List<Expr> exprs = new ArrayList<>();
			exprs.add(expr1);
			exprs.add(expr2);
			return new Or(exprs);
		}
	}

	@Override
	public Expr fold() {
		// this code should be maintained in parallel with And.fold
		var new_exprs = new ArrayList<Expr>(exprs.size());
		for (var e : exprs) {
			e = e.fold();
			var c = e.constant();
			if (c == Boolean.TRUE)
				return Constant.TRUE;
			if (c != Boolean.FALSE)
				new_exprs.add(e);
		}
		if (new_exprs.isEmpty())
			return Constant.FALSE;
		exprs = new_exprs;
		return this;
	}

	@Override
	public Object eval(Header hdr, Row row) {
		for (Expr e : exprs)
			if (Ops.toBoolean_(e.eval(hdr, row)))
				return Boolean.TRUE;
		return Boolean.FALSE;
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		List<Expr> new_exprs = renameExprs(from, to);
		return new_exprs == null ? this : new Or(new_exprs);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		List<Expr> new_exprs = replaceExprs(from, to);
		return new_exprs == null ? this : new Or(new_exprs);
	}

}
