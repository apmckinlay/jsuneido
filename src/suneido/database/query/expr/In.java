package suneido.database.query.expr;

import static suneido.Util.listToParens;

import java.util.ArrayList;
import java.util.List;

import suneido.SuValue;

public class In extends Expr {
	private final Expr expr;
	private final List<SuValue> values = new ArrayList<SuValue>();

	public In(Expr expr) {
		this.expr = expr;
	}

	public In add(SuValue x) {
		values.add(x);
		return this;
	}

	@Override
	public String toString() {
		return "(" + expr + " in " + listToParens(values) + ")";
	}

	@Override
	public List<String> fields() {
		// TODO Auto-generated method stub
		return null;
	}

}
