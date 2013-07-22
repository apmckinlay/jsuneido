/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

public class Synchronized {

	@Params("block")
	public static Object Synchronized(Object a) {
		synchronized (Synchronized.class) {
			return Ops.call(a);
		}
	}

}
