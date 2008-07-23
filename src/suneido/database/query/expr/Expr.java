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

	public Expr rename(List<String> from, List<String> to) {
		return this;
	}

	public Expr replace(List<String> from, List<Expr> to) {
		return this;
	}

	public boolean isTerm(List<String> fields) {
		return false; // TODO override appropriately
	}

	public boolean isField(List<String> fields) {
		return false;
	}

	public abstract SuValue eval(Header hdr, Row row);

}
