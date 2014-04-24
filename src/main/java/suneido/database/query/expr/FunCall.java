/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import static suneido.util.Util.addAllUnique;
import static suneido.util.Util.displayListToParens;

import java.util.List;

import suneido.SuException;
import suneido.Suneido;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Ops;

public class FunCall extends Multi {
	private final Expr ob;
	private final String fname;

	public FunCall(Expr ob, String fname) {
		this.ob = ob;
		this.fname = fname;
	}

	public FunCall(Expr ob, String fname, List<Expr> exprs) {
		super(exprs);
		this.ob = ob;
		this.fname = fname;
	}

	@Override
	public String toString() {
		return (ob == null ? "" : ob + ".") + fname + displayListToParens(exprs);
	}

	@Override
	public List<String> fields() {
		List<String> f = super.fields();
		if (ob != null)
			addAllUnique(f, ob.fields());
		return f;
	}

	@Override
	public Expr fold() {
		List<Expr> new_exprs = foldExprs(null);
		return new_exprs == null ? this : new FunCall(ob, fname, new_exprs);
	}

	@Override
	public Object eval(Header hdr, Row row) {
		Object[] args = new Object[exprs.size()];
		int i = 0;
		for (Expr e : exprs)
			args[i++] = e.eval(hdr, row);
		Object result;
		if (ob == null)
			result = Ops.call(Suneido.context.get(fname), args);
		else {
			Object x = ob.eval(hdr, row);
			result = Ops.invoke(x, fname, args);
		}
		if (result == null)
			throw new SuException("no return value from " + fname);
		//System.out.println("Eval " + fname + Ops.display(args) + " => " + result);
		return result;
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		Expr new_ob = (ob == null) ? ob : ob.rename(from, to);
		List<Expr> new_exprs = renameExprs(from, to);
		if (new_ob == ob && new_exprs == null)
			return this;
		if (new_exprs == null)
			new_exprs = exprs;
		return new FunCall(new_ob, fname, new_exprs);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		Expr new_ob = (ob == null) ? ob : ob.replace(from, to);
		List<Expr> new_exprs = replaceExprs(from, to);
		if (new_ob == ob && new_exprs == null)
			return this;
		if (new_exprs == null)
			new_exprs = exprs;
		return new FunCall(new_ob, fname, new_exprs);
	}
}
