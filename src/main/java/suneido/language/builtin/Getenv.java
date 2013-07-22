/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Getenv {

	@Params("string")
	public static String Getenv(Object a) {
		String name = Ops.toStr(a);
		return System.getenv(name);
	}

}
