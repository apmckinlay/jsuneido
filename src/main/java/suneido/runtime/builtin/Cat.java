/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Cat {

	@Params("string1, string2")
	public static Object Cat(Object a, Object b) {
		return Ops.cat(a, b);
	}

}
