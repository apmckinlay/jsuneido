package suneido.database.query.expr;

import java.util.List;

import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public abstract class Expr {

	@Override
	public abstract String toString();

	public abstract List<String> fields();

	public Expr fold() {
		return this;
	}

	public abstract void rename(List<String> from, List<String> to);

	public abstract Expr replace(List<String> from, List<Expr> to);

	public boolean isTerm(List<String> fields) {
		return false; // TODO override appropriately
	}

	public boolean isField(List<String> fields) {
		return false;
	}

	public abstract SuValue eval(Header hdr, Row row);

}
