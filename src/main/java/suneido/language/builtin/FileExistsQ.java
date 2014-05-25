/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;

import suneido.language.Ops;
import suneido.language.Params;

public class FileExistsQ {

	@Params("filename")
	public static Boolean FileExistsQ(Object filename) {
		return new File(Ops.toStr(filename)).exists();
	}

}
