/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;

public class SameQ {

	@Params("x, y")
	public static Object SameQ(Object x, Object y) {
		return x == y;
	}

}
