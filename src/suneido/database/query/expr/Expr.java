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

	public Expr replace(List<String> from, List<String> to) {
		return this;
	}
}
