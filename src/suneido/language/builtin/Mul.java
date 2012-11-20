/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Mul {

	@Params("number1, number2")
	public static Number Mul(Object a, Object b) {
		return Ops.mul(a, b);
	}

}
