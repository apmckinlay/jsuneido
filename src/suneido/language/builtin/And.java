/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class And {

	@Params("boolean, boolean")
	public static boolean And(Object a, Object b) {
		return Ops.toBoolean_(a) && Ops.toBoolean_(b);
	}

}
