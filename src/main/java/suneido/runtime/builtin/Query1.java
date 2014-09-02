/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.database.query.Query.Dir;
import suneido.runtime.Ops;
import suneido.runtime.SuFunction;

public class Query1 extends SuFunction {

	@Override
	public Object call(Object... args) {
		return SuTransaction.queryOne(null, args, Dir.NEXT, true);
	}

	@Override
	public Object call1(Object a) {
		return SuTransaction.queryOne(null, Ops.toStr(a), Dir.NEXT, true);
	}

}
