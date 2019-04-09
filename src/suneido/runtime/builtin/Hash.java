/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;

public class Hash {

	@Params("value")
	public static Object Hash(Object value) {
		return value.hashCode();
	}

}
