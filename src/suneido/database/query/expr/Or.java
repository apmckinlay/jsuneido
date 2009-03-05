package suneido.database.query.expr;

import java.util.ArrayList;
import java.util.List;

import suneido.SuBoolean;
import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

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
			List<Expr> exprs = new ArrayList<Expr>();
			exprs.add(expr1);
			exprs.add(expr2);
			return new Or(exprs);
		}
	}

	@Override
	public Expr fold() {
		List<Expr> new_exprs = foldExprs(Constant.FALSE);
		boolean allfalse = true;
		for (Expr e : new_exprs == null ? exprs : new_exprs)
			if (e == Constant.TRUE)
				return Constant.TRUE;
			else
				allfalse = false;
		if (allfalse)
			return Constant.FALSE;
		return new_exprs == null ? this : new Or(new_exprs);
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		for (Expr e : exprs)
			if (e.eval(hdr, row) == SuBoolean.TRUE)
				return SuBoolean.TRUE;
		return SuBoolean.FALSE;
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
