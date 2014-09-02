/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.nio.ByteBuffer;

import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.ByteBuffers;

public class Unpack {

	@Params("value")
	public static Object Unpack(Object a) {
		String s = Ops.toStr(a);
		ByteBuffer buf = ByteBuffers.stringToBuffer(s);
		return suneido.runtime.Pack.unpack(buf);
	}

}
