/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;
import java.io.IOException;

import suneido.runtime.Ops;
import suneido.runtime.Params;

import com.google.common.io.Files;

public class CopyFile {

	@Params("from, to, failIfExists")
	public static boolean CopyFile(Object a, Object b, Object c) {
		File from = new File(Ops.toStr(a));
		File to = new File(Ops.toStr(b));
		boolean failIfExists = Ops.toBoolean(c);
		if (to.exists() && (failIfExists || ! to.delete()))
			return false;
		try {
			Files.copy(from, to);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
