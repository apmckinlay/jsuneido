/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.database.query.Query.Dir;
import suneido.runtime.Ops;
import suneido.runtime.SuBuiltinFunction;

public final class Query1 extends SuBuiltinFunction {

	public Query1() {
		super("Query1", SuTransaction.queryOneFS);
	}

	@Override
	public Object call(Object... args) {
		return SuTransaction.queryOne(null, args, Dir.NEXT, true);
	}

	@Override
	public Object call1(Object a) {
		return SuTransaction.queryOne(null, Ops.toStr(a), Dir.NEXT, true);
	}
}
