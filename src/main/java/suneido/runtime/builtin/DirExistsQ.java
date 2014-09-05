/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class DirExistsQ {

	@Params("string")
	public static Boolean DirExistsQ(Object dir) {
		return new File(Ops.toStr(dir)).isDirectory();
	}

}
