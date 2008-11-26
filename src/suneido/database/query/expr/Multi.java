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

	protected List<Expr> foldExprs(Expr ignore) {
		ArrayList<Expr> new_exprs = new ArrayList<Expr>();
		boolean changed = false;
		for (Expr e : exprs) {
			Expr e2 = e.fold();
			if (e2 != ignore)
				new_exprs.add(e2);
			if (e2 != e)
				changed = true;
		}
		return changed || new_exprs.size() != exprs.size() ? new_exprs : null;
	}

	protected List<Expr> renameExprs(List<String> from, List<String> to) {
		ArrayList<Expr> new_exprs = new ArrayList<Expr>();
		boolean changed = false;
		for (Expr e : exprs) {
			Expr e2 = e.rename(from, to);
			new_exprs.add(e2);
			if (e2 != e)
				changed = true;
		}
		return changed ? new_exprs : null;
	}

	protected List<Expr> replaceExprs(List<String> from, List<Expr> to) {
		ArrayList<Expr> new_exprs = new ArrayList<Expr>();
		boolean changed = false;
		for (Expr e : exprs) {
			Expr e2 = e.replace(from, to);
			new_exprs.add(e2);
			if (e2 != e)
				changed = true;
		}
		return changed ? new_exprs : null;
	}
}
