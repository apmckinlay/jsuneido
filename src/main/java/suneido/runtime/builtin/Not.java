/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Not {

	@Params("boolean")
	public static Boolean Not(Object a) {
		return Ops.not(a);
	}

}
