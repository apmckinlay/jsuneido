/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class And {

	@Params("boolean1, boolean2")
	public static boolean And(Object a, Object b) {
		return Ops.toBoolean_(a) && Ops.toBoolean_(b);
	}

}
