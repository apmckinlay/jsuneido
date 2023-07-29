/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class CreateDir {

	@Params("string")
	public static Object CreateDir(Object a) {
		String path = Ops.toStr(a);
		return new File(path).mkdir();
	}

}
