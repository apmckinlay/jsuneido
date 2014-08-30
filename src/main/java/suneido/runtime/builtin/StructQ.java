/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.jsdi.DllInterface;
import suneido.jsdi.type.Structure;
import suneido.runtime.Params;

/**
 * Built-in function {@code Struct?(value)}. Returns {@code true} if-and-only-if
 * the given value is a {@code struct}.
 * @author Victor Schappert
 * @since 20130709
 * @see DllQ
 */
@DllInterface
public final class StructQ {

	@Params("value")
	public static Boolean StructQ(Object a) {
		return a instanceof Structure;
	}

}
