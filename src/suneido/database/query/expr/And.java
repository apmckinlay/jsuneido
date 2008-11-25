package suneido.database.query.expr;

import java.util.List;

import suneido.SuBoolean;
import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class And extends Multi {

	public And() {
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

	@Override
	public SuValue eval(Header hdr, Row row) {
		for (Expr e : exprs)
			if (e.eval(hdr, row) != SuBoolean.TRUE)
				return SuBoolean.FALSE;
		return SuBoolean.TRUE;
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		List<Expr> new_exprs = rename_exprs(from, to);
		return new_exprs == null ? this : new And(new_exprs);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		List<Expr> new_exprs = replace_exprs(from, to);
		return new_exprs == null ? this : new And(new_exprs);
	}

}
