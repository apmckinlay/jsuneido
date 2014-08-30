/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;
import suneido.util.Util;

public class Pack {

	@Params("value")
	public static String Pack(Object a) {
		return Util.bytesToString(suneido.runtime.Pack.pack(a));
	}

}
