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

	protected Expr fold_exprs(Constant ignore, Constant target) {
		Expr x = checkFold(ignore, target);
		if (x != null)
			return x;
		List<Expr> new_exprs = new ArrayList<Expr>();
		for (Expr e : exprs) {
			Expr new_e = e.fold();
			if (new_e != ignore)
				new_exprs.add(new_e);
		}
		exprs = new_exprs;
		return exprs.isEmpty() ? ignore : this;
	}
	private Expr checkFold(Constant ignore, Constant target) {
		for (Expr e : exprs) {
			if (e == ignore)
				return null;
			Expr new_e = e.fold();
			if (new_e == target)
				return target;
			if (new_e != e)
				return null;
		}
		return this;
	}

}
