package suneido.database.query.expr;

import static suneido.util.Util.listToParens;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import suneido.database.Record;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Ops;

public class In extends Expr {
	public final Expr expr;
	private final List<Object> values;
	public final Record packed;
	private Boolean isterm = null;

	public In(Expr expr) {
		this(expr, new ArrayList<Object>(), new Record());
	}

	public In(Expr expr, List<Object> values, Record packed) {
		this.expr = expr;
		this.values = values;
		this.packed = packed;
	}

	public In add(Object x) {
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
		Expr new_expr = expr.fold();
		if (new_expr instanceof Constant)
			return Constant.valueOf(eval2(((Constant) new_expr).value));
		return new_expr == expr ? this : new In(new_expr, values, packed);
	}

	@Override
	public boolean isTerm(List<String> fields) {
		return expr.isField(fields);
	}

	@Override
	public Object eval(Header hdr, Row row) {
		// once we're eval'ing it is safe to cache isTerm
		if (isterm == null)
			isterm = isTerm(hdr.columns());
		if (isterm) {
			Identifier id = (Identifier) expr;
			ByteBuffer value = row.getraw(hdr, id.ident);
			for (ByteBuffer v : packed)
				if (v.equals(value))
					return Boolean.TRUE;
			return Boolean.FALSE;
		} else {
			Object x = expr.eval(hdr, row);
			return eval2(x);
		}
	}

	private Object eval2(Object x) {
		for (Object y : values)
			if (Ops.is(x, y))
				return Boolean.TRUE;
		return Boolean.FALSE;
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		Expr new_expr = expr.rename(from, to);
		return new_expr == expr ? this : new In(new_expr, values, packed);
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		Expr new_expr = expr.replace(from, to);
		return new_expr == expr ? this : new In(new_expr, values, packed);
	}
}
