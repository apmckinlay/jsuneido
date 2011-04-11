package suneido.database.query.expr;

import static suneido.util.Util.displayListToParens;

import java.util.List;

import suneido.SuException;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Globals;
import suneido.language.Ops;

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
		return fname + displayListToParens(exprs);
	}

	@Override
	public Expr fold() {
		List<Expr> new_exprs = foldExprs(null);
		return new_exprs == null ? this : new FunCall(fname, new_exprs);
	}

	@Override
	public Object eval(Header hdr, Row row) {
		Object[] args = new Object[exprs.size()];
		int i = 0;
		for (Expr e : exprs)
			args[i++] = e.eval(hdr, row);
		Object result = Ops.call(Globals.get(fname), args);
		if (result == null)
			throw new SuException("no return value from " + fname);
		//System.out.println("Eval " + fname + Ops.display(args) + " => " + result);
		return result;
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
