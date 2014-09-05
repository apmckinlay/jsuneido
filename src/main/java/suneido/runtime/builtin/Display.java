/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Display {

	@Params("value")
	public static Object Display(Object a) {
		return Ops.display(a);
	}

}
