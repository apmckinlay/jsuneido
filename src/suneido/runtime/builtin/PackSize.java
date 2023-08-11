/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;
import suneido.util.Util;

public class PackSize {

	@Params("value")
	public static int PackSize(Object a) {
		return suneido.runtime.Pack.packSize(a);
	}

}
