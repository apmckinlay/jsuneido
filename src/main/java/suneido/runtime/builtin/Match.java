/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Match {

	@Params("string, pattern")
	public static Boolean Match(Object a, Object b) {
		return Ops.match(a, b);
	}

}
