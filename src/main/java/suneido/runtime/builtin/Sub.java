/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Sub {

	@Params("number1, number2")
	public static Number Sub(Object a, Object b) {
		return Ops.sub(a, b);
	}

}