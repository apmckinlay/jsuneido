/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Random {

	private static final java.util.Random random = new java.util.Random();

	@Params("number")
	public static Integer Random(Object a) {
		int n = Ops.toInt(a);
		return n == 0 ? 0 : random.nextInt(n);
	}

}
