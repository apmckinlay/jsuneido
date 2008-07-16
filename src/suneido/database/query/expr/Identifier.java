package suneido.database.query.expr;

import java.util.List;

import suneido.SuBoolean;

public class Identifier extends Expr {
	private final String s;

	public static Expr valueOf(String s) {
		if (s.equals("true"))
			return new Constant(SuBoolean.TRUE);
		else if (s.equals("false"))
			return new Constant(SuBoolean.FALSE);
		else
			return new Identifier(s);
	}

	public Identifier(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}

	@Override
	public List<String> fields() {
		// TODO Auto-generated method stub
		return null;
	}

}
