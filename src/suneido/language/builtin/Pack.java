/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Params;
import suneido.util.Util;

public class Pack {

	@Params("value")
	public static String Pack(Object a) {
		return Util.bytesToString(suneido.language.Pack.pack(a));
	}

}
