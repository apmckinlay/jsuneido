package suneido.database.query.expr;

import static suneido.Util.listToParens;

public class FunCall extends Multi {
	private final String fname;

	public FunCall(String fname) {
		this.fname = fname;
	}

	@Override
	public String toString() {
		return fname + listToParens(exprs);
	}

}
