package suneido.database.query.expr;

import java.util.Collections;
import java.util.List;

import suneido.SuBoolean;

public class Identifier extends Expr {
	public final String ident;

	public static Expr valueOf(String s) {
		if (s.equals("true"))
			return new Constant(SuBoolean.TRUE);
		else if (s.equals("false"))
			return new Constant(SuBoolean.FALSE);
		else
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
	public boolean isfield(List<String> fields) {
		return fields.contains(ident);
	}
}
