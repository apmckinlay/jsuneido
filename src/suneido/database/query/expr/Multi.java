package suneido.database.query.expr;

import static suneido.Util.addUnique;

import java.util.ArrayList;
import java.util.List;

public abstract class Multi extends Expr {
	public List<Expr> exprs;

	public Multi() {
		exprs = new ArrayList<Expr>();
	}

	public Multi(List<Expr> exprs) {
		this.exprs = exprs;
	}

	public Multi add(Expr e) {
		assert e != null;
		exprs.add(e);
		return this;
	}

	protected String toString(String op) {
		if (exprs.isEmpty())
			return "";
		if (exprs.size() == 1)
			return exprs.get(0).toString();
		String s = "(";
		for (Expr e : exprs)
			s += e + op;
		return s.substring(0, s.length() - op.length()) + ")";
	}

	@Override
	public List<String> fields() {
		List<String> f = new ArrayList<String>();
		for (Expr e : exprs)
			addUnique(f, e.fields());
		return f;
	}

	protected Expr foldExprs(Constant ignore, Constant target) {
		// iterate reverse to simplify deleting
		for (int i = exprs.size() - 1; i >= 0; --i) {
			Expr e = exprs.get(i).fold();
			if (e == target)
				return target;
			else if (e == ignore)
				exprs.remove(i);
			else
				exprs.set(i, e);
		}
		return exprs.isEmpty() ? ignore : this;
	}

	protected List<Expr> rename_exprs(List<String> from, List<String> to) {
		ArrayList<Expr> exprs2 = new ArrayList<Expr>();
		boolean changed = false;
		for (Expr e : exprs) {
			Expr e2 = e.rename(from, to);
			exprs2.add(e2);
			if (e2 != e)
				changed = true;
		}
		return changed ? exprs2 : null;
	}

	protected List<Expr> replace_exprs(List<String> from, List<Expr> to) {
		ArrayList<Expr> exprs2 = new ArrayList<Expr>();
		boolean changed = false;
		for (Expr e : exprs) {
			Expr e2 = e.replace(from, to);
			exprs2.add(e2);
			if (e2 != e)
				changed = true;
		}
		return changed ? exprs2 : null;
	}
}
