/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Display {

	@Params("value")
	public static Object Display(Object a) {
		return Ops.display(a);
	}

}
