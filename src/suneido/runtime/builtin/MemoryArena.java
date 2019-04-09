/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.util.Dnum;

public class MemoryArena {

	public static Object MemoryArena() {
		return Dnum.from(Runtime.getRuntime().totalMemory());
	}

}
