/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

// needed because Suneido code can't catch block return
public class Finally {

	@Params("main, final")
	public static Object Finally(Object main, Object fin) {
		Object result;
		try {
			result = Ops.call0(main);
		} catch (Throwable e1) {
			try {
				Ops.call0(fin);
			} catch (Throwable e2) {
				// ignore exception in final block if exception in main block
			}
			throw e1;
		}
		Ops.call0(fin);
		return result;
	}

}
