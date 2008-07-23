package suneido.database.query.expr;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import suneido.SuBoolean;
import suneido.SuInteger;
import suneido.SuString;
import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;

public class Constant extends Expr {
	public final SuValue value;
	public final ByteBuffer packed;
	public static final Constant TRUE = new Constant(SuBoolean.TRUE);
	public static final Constant FALSE = new Constant(SuBoolean.FALSE);
	public static final Constant EMPTY = new Constant(SuString.EMPTY);
	public static final Constant ZERO = new Constant(SuInteger.ZERO);
	public static final Constant ONE = new Constant(SuInteger.ONE);

	public static Constant valueOf(SuValue value) {
		if (value == SuBoolean.TRUE)
			return TRUE;
		else if (value == SuBoolean.FALSE)
			return FALSE;
		else if (value == SuString.EMPTY)
			return EMPTY;
		else if (value == SuInteger.ZERO)
			return ZERO;
		else if (value == SuInteger.ONE)
			return ONE;
		else
			return new Constant(value);
	}

	private Constant(SuValue value) {
		this.value = value;
		packed = value.pack();
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public List<String> fields() {
		return Collections.emptyList();
	}

	@Override
	public SuValue eval(Header hdr, Row row) {
		return value;
	}

	@Override
	public void rename(List<String> from, List<String> to) {
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		return this;
	}

}
