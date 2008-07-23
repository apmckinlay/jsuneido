package suneido.database.query.expr;

import static suneido.Util.listToParens;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import suneido.SuBoolean;
import suneido.SuValue;
import suneido.database.Record;
import suneido.database.query.Header;
import suneido.database.query.Row;

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
		if (expr instanceof Constant)
			return Constant.valueOf(eval2(((Constant) expr).value));
		return this;
	}

	@Override
	public boolean isTerm(List<String> fields) {
		return expr.isField(fields);
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		if (isTerm(hdr.columns())) {
			Identifier id = (Identifier) expr;
			ByteBuffer value = row.getraw(hdr, id.ident);
			for (ByteBuffer v : packed)
				if (v.equals(value))
					return SuBoolean.TRUE;
			return SuBoolean.FALSE;
		} else {
			SuValue x = expr.eval(hdr, row);
			return eval2(x);
		}
	}

	private SuValue eval2(SuValue x) {
		for (SuValue y : values)
			if (x.equals(y))
				return SuBoolean.TRUE;
		return SuBoolean.FALSE;
	}
}
