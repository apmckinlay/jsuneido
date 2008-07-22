package suneido.database.query.expr;

import static suneido.Util.listToParens;

import java.util.ArrayList;
import java.util.List;

import suneido.SuValue;
import suneido.database.Record;

public class In extends Expr {
	private Expr expr;
	private final List<SuValue> values = new ArrayList<SuValue>();
	private final Record packed = new Record();

	public In(Expr expr) {
		this.expr = expr;
	}

	public In add(SuValue x) {
		values.add(x);
		packed.add(x);
		return this;
	}

	@Override
	public String toString() {
		return expr + " in " + listToParens(values);
	}

	@Override
	public List<String> fields() {
		return expr.fields();
	}

	@Override
	public Expr fold() {
		expr = expr.fold();
		if (expr instanceof Constant) {
			SuValue x = ((Constant) expr).value;
			for (SuValue y : values)
				if (x.equals(y))
					return Constant.TRUE;
			return Constant.FALSE;
		}
		return this;
	}

	@Override
	public boolean isTerm(List<String> fields) {
		return expr.isField(fields);
	}
}
