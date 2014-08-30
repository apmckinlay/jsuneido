/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toStr;

import java.io.File;

import suneido.runtime.Params;

public class DeleteFile {

	@Params("filename")
	public static Boolean DeleteFile(Object filename) {
		return new File(toStr(filename)).delete();
	}

}
