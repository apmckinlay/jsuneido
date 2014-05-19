/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.database.query.Query.Dir;
import suneido.language.Ops;
import suneido.language.SuFunction;

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
