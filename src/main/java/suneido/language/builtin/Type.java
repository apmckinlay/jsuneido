/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Type {

	@Params("value")
	public static String Type(Object a) {
		return Ops.typeName(a);
	}

}
