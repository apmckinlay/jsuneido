/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.jsdi.Dll;
import suneido.jsdi.DllInterface;
import suneido.runtime.Params;

/**
 * Built-in function {@code Dll?(value)}. Returns {@code true} if-and-only-if
 * the given value is a {@code dll}.
 * @author Victor Schappert
 * @since 20130709
 * @see StructQ
 * @see BufferQ
 */
@DllInterface
public final class DllQ {

	@Params("value")
	public static Boolean DllQ(Object a) {
		return a instanceof Dll;
	}

}
