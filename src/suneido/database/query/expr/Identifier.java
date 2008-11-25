package suneido.database.query.expr;

import java.util.Collections;
import java.util.List;

import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class Identifier extends Expr {
	public String ident;

	public static Expr valueOf(String s) {
		return new Identifier(s);
	}

	public Identifier(String ident) {
		this.ident = ident;
	}

	@Override
	public String toString() {
		return ident;
	}

	@Override
	public List<String> fields() {
		return Collections.singletonList(ident);
	}

	@Override
	public boolean isField(List<String> fields) {
		return fields.contains(ident);
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		return row.getval(hdr, ident);
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		int i = from.indexOf(ident);
		return i == -1 ? this : new Identifier(to.get(i));
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		int i = from.indexOf(ident);
		return i == -1 ? this : to.get(i);
	}
}
