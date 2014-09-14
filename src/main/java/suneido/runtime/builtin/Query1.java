/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.database.query.Query.Dir;
import suneido.runtime.Ops;
import suneido.runtime.SuBuiltinFunction1;

public final class Query1 extends SuBuiltinFunction1 {

	public Query1() {
		super("Query1", SuTransaction.queryOneFS);
	}

	@Override
	public Object call1(Object a) {
		return SuTransaction.queryOne(null, Ops.toStr(a), Dir.NEXT, true);
	}
}
