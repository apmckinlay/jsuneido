/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Getenv {

	@Params("string")
	public static String Getenv(Object a) {
		String name = Ops.toStr(a);
		return System.getenv(name);
	}

}
