package suneido.database.query.expr;

import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;

public abstract class Expr {

	@Override
	public abstract String toString();

	public abstract List<String> fields();

	public Expr fold() {
		return this;
	}

	public abstract Expr rename(List<String> from, List<String> to);

	public abstract Expr replace(List<String> from, List<Expr> to);

	public boolean isTerm(List<String> fields) {
		return false; // override appropriately in derived classes
	}

	public boolean isField(List<String> fields) {
		return false;
	}

	public abstract Object eval(Header hdr, Row row);

}
