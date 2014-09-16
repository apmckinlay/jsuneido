/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class StringQ {

	@Params("value")
	public static Boolean StringQ(Object a) {
		return Ops.isString(a);
	}

}
