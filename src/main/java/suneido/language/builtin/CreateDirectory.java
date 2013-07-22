/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;

import suneido.language.Ops;
import suneido.language.Params;

public class CreateDirectory {

	@Params("string")
	public static Object CreateDirectory(Object a) {
		String path = Ops.toStr(a);
		return new File(path).mkdir();
	}

}
