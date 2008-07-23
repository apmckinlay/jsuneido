package suneido.database.query.expr;

import suneido.SuBoolean;
import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class Or extends Multi {

	@Override
	public String toString() {
		return super.toString(" or ");
	}

	@Override
	public Expr fold() {
		return foldExprs(Constant.FALSE, Constant.TRUE);
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		for (Expr e : exprs)
			if (e.eval(hdr, row) == SuBoolean.TRUE)
				return SuBoolean.TRUE;
		return SuBoolean.FALSE;
	}

}
