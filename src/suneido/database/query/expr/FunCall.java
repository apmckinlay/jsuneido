package suneido.database.query.expr;

import static suneido.util.Util.listToParens;

import java.util.List;

import suneido.SuException;
import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class FunCall extends Multi {
	private final String fname;

	public FunCall(String fname) {
		this.fname = fname;
	}

	public FunCall(String fname, List<Expr> exprs) {
		super(exprs);
		this.fname = fname;
	}

	@Override
	public String toString() {
		return fname + listToParens(exprs);
	}

	@Override
	public Expr fold() {
		List<Expr> new_exprs = foldExprs(null);
		return new_exprs == null ? this : new FunCall(fname, new_exprs);
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		// TODO FunCall eval
		throw new SuException("not implemented: FunCall");
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		List<Expr> new_exprs = renameExprs(from, to);
		return new_exprs == null ? this : new FunCall(fname, new_exprs);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		List<Expr> new_exprs = replaceExprs(from, to);
		return new_exprs == null ? this : new FunCall(fname, new_exprs);
	}
}
