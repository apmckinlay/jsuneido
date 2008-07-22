package suneido.database.query.expr;

import java.util.Collections;
import java.util.List;

public class Identifier extends Expr {
	public final String ident;

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
}
