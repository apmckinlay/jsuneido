/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.language.Ops;
import suneido.language.Pack;

public class Constant extends Expr {
	public final Object value;
	public final ByteBuffer packed;
	public static final Constant TRUE = new Constant(Boolean.TRUE);
	public static final Constant FALSE = new Constant(Boolean.FALSE);
	public static final Constant EMPTY = new Constant("");
	public static final Constant ZERO = new Constant(0);
	public static final Constant ONE = new Constant(1);

	public static Constant valueOf(Object value) {
		if (value == Boolean.TRUE)
			return TRUE;
		else if (value == Boolean.FALSE)
			return FALSE;
		else if ("".equals(value))
			return EMPTY;
		else if (ZERO.value.equals(value))
			return ZERO;
		else if (ONE.value.equals(value))
			return ONE;
		else
			return new Constant(value);
	}

	private Constant(Object value) {
		this.value = value;
		packed = Pack.pack(value);
	}

	@Override
	public String toString() {
		return Ops.display(value);
	}

	@Override
	public List<String> fields() {
		return Collections.emptyList();
	}

	@Override
	public Object eval(Header hdr, Row row) {
		return value;
	}

	@Override
	public Expr rename(List<String> from, List<String> to) {
		return this;
	}

	@Override
	public Expr replace(List<String> from, List<Expr> to) {
		return this;
	}

}
