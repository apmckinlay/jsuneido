/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query.expr;

import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;

public abstract class Expr {

	@Override
	public abstract String toString();

	public abstract List<String> fields();

	public Expr fold() {
		return this;
	}

	public abstract Expr rename(List<String> from, List<String> to);

	public abstract Expr replace(List<String> from, List<Expr> to);

	/**
	 * Determines if an operation can be done "raw" i.e. without unpacking.
	 * Overridden by {@link BinOp} and {@link In}.
	 * This is used by {@link Select} optimization.
	 * <p>
	 * Note: Should only be used for "simple" source/header.
	 * e.g. Doesn't work with union when one source has field and other has rule.
	 */
	public boolean isTerm(List<String> fields) {
		return false;
	}

	public boolean isField(List<String> fields) {
		return false;
	}

	public abstract Object eval(Header hdr, Row row);

}
