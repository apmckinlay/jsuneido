/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class FileExistsQ {

	@Params("filename")
	public static Boolean FileExistsQ(Object filename) {
		return new File(Ops.toStr(filename)).exists();
	}

}
