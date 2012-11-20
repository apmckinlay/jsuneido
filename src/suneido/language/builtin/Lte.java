/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Lte {

	@Params("value1, value2")
	public static Boolean Lte(Object a, Object b) {
		return Ops.lte(a, b);
	}

}
