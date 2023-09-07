/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toStr;

import java.io.File;

import suneido.runtime.Params;

public class DeleteFileApi {

	@Params("filename")
	public static Object DeleteFileApi(Object filename) {
		File file = new File(toStr(filename));
		if (!file.exists())
			return "DeleteFile " + filename + ": does not exist";
		if (!file.isFile())
			return "DeleteFile " + filename + ": not a file";
		return file.delete();
	}

}
