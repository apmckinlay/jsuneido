/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Neg {

	@Params("number")
	public static Number Neg(Object a) {
		return Ops.uminus(a);
	}

}
