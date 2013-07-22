/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.Ops.toInt;
import suneido.language.Params;

public class Exit {

	@Params("status = 0")
	public static Object Exit(Object status) {
		System.exit(toInt(status));
		return null;
	}

}
