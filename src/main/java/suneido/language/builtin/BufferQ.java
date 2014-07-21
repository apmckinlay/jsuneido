/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.language.Params;

/**
 * Built-in function {@code Buffer?(value)}. Returns {@code true} if-and-only-if
 * the given value is a {@code Buffer} instance.
 * @author Victor Schappert
 * @since 201301214
 * @see StructQ
 * @see DllQ
 */
@DllInterface
public final class BufferQ {

	@Params("value")
	public static Boolean BufferQ(Object a) {
		return a instanceof Buffer;
	}

}
