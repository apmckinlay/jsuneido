/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Cat {

	@Params("string1, string2")
	public static Object Cat(Object a, Object b) {
		return Ops.cat(a, b);
	}

}
