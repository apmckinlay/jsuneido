/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;

public class NumberQ {

	@Params("value")
	public static Boolean NumberQ(Object a) {
		return a instanceof Number;
	}

}
