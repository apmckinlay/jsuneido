package suneido.database.query.expr;

import java.util.Collections;
import java.util.List;

import suneido.SuValue;

public class Constant extends Expr {
	final SuValue value;

	public Constant(SuValue value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public List<String> fields() {
		return Collections.emptyList();
	}

}
