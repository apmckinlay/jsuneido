/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Util;

public class Sleep {

	@Params("ms")
	public static Object Sleep(Object a) {
		int ms = Ops.toInt(a);
		Util.interruptableSleep(ms);
		return null;
	}

}
