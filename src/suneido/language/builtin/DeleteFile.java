/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.Ops.toStr;

import java.io.File;

import suneido.language.Params;

public class DeleteFile {

	@Params("filename")
	public static Boolean DeleteFile(Object filename) {
		return new File(toStr(filename)).delete();
	}

}
