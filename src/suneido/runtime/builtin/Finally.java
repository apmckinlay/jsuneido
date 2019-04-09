/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;

// needed because Suneido code cannot catch block return
public class Finally {

	@Params("main_block, final_block")
	public static Object Finally(Object main, Object fin) {
		try {
			Object result = Ops.call0(main);
			Ops.call0(fin); // could throw
			return result;
		} catch (Throwable e) {
			try {
				Ops.call0(fin);
			} catch (Throwable e2) {
				// ignore exception from final_block if main_block threw
			}
			throw e;
		}
	}

}
