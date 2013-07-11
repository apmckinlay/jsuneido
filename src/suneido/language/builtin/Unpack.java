/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.nio.ByteBuffer;

import suneido.language.Ops;
import suneido.language.Params;
import suneido.util.ByteBuffers;

public class Unpack {

	@Params("value")
	public static Object Unpack(Object a) {
		String s = Ops.toStr(a);
		ByteBuffer buf = ByteBuffers.stringToBuffer(s);
		return suneido.language.Pack.unpack(buf);
	}

}
