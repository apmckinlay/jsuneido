/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

 package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Cmp {

	@Params("x, y")
	public static Object Cmp(Object x, Object y) {
		return Ops.cmp(x, y);
	}

}
