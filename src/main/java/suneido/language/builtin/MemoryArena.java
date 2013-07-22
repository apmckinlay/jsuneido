/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

public class MemoryArena {

	public static long MemoryArena() {
		return Runtime.getRuntime().totalMemory();
	}

}
