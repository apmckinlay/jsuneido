package suneido.database.query.expr;

import java.util.List;

public abstract class Expr {

	@Override
	public abstract String toString();

	public abstract List<String> fields();

	// public abstract Expr fold();
}
