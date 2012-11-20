/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class StringQ {

	@Params("value")
	public static Boolean StringQ(Object a) {
		return Ops.isString(a);
	}

}
