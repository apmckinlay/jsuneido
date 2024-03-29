/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toInt;

import suneido.Suneido;
import suneido.runtime.Params;

public class Exit {

	@Params("status = 0")
	public static Object Exit(Object status) {
		if (status == Boolean.TRUE)
			status = 1;
		Suneido.exit(toInt(status));
		return null;
	}

}
