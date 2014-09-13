/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.database.query.Query.Dir;
import suneido.runtime.SuCallBase;

public class QueryLast extends SuCallBase {

	@Override
	public Object call(Object... args) {
		return SuTransaction.queryOne(null, args, Dir.PREV, false);
	}

}
