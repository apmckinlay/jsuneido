package suneido.database.query.expr;

import java.util.List;

import suneido.SuValue;

public class Constant extends Expr {
	private final SuValue value;

	public Constant(SuValue value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public List<String> fields() {
		// TODO Auto-generated method stub
		return null;
	}

}
