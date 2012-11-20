/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Add {

	@Params("number1, number2")
	public static Number Add(Object a, Object b) {
		return Ops.add(a, b);
	}

}
