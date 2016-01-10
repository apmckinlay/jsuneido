/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Sleep {

	@Params("ms")
	public static Object Sleep(Object a) {
		int n = Ops.toInt(a);
		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

}
