/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;


public class BooleanQ {

	@Params("value")
	public static boolean BooleanQ(Object a) {
		return a instanceof Boolean;
	}

}
