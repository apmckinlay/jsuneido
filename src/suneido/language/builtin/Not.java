/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Not {

	@Params("boolean")
	public static Boolean Not(Object a) {
		return Ops.not(a);
	}

}
