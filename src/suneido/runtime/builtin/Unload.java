/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Suneido;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Unload {

	@Params("name = false")
	public static Object Unload(Object a) {
		if (a == Boolean.FALSE)
			Suneido.context.clearAll();
		else
			Suneido.context.clear(Ops.toStr(a));
		return null;
	}

}
