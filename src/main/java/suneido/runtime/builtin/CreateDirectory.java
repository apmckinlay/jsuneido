/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class CreateDirectory {

	@Params("string")
	public static Object CreateDirectory(Object a) {
		String path = Ops.toStr(a);
		return new File(path).mkdir();
	}

}
