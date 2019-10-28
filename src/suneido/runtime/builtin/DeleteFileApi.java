/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toStr;

import java.io.File;

import suneido.runtime.Params;

public class DeleteFileApi {

	@Params("filename")
	public static Boolean DeleteFileApi(Object filename) {
		File file = new File(toStr(filename));
		if (!file.isFile())
			return false;
		return file.delete();
	}

}
