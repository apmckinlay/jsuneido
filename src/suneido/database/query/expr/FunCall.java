package suneido.database.query.expr;

import static suneido.Util.listToParens;
import suneido.SuException;
import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class FunCall extends Multi {
	private final String fname;

	public FunCall(String fname) {
		this.fname = fname;
	}

	@Override
	public String toString() {
		return fname + listToParens(exprs);
	}

	@Override
	public Expr fold() {
		for (int i = 0; i < exprs.size(); ++i)
			exprs.set(i, exprs.get(i).fold());
		return this;
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		// TODO FunCall eval
		throw new SuException("not implemented: FunCall");
	}
}
