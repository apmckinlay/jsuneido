/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Div {

	@Params("number1, number2")
	public static Number Div(Object a, Object b) {
		return Ops.div(a, b);
	}

}
