package suneido.database.query.expr;

import java.util.List;

public abstract class Expr {

	@Override
	public abstract String toString();

	public abstract List<String> fields();

	public Expr fold() {
		return this;
	}

	public Expr rename(List<String> from, List<String> to) {
		return this;
	}

	public Expr replace(List<String> from, List<Expr> to) {
		return this;
	}

	public boolean is_term(List<String> fields) {
		return false; // TODO override appropriately
	}

	public boolean isfield(List<String> fields) {
		// TODO Auto-generated method stub
		return false;
	}
}
