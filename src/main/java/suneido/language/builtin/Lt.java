/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Lt {

	@Params("value1, value2")
	public static Boolean Lt(Object a, Object b) {
		return Ops.lt(a, b);
	}

}
