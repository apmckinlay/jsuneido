/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.nio.file.*;
import java.io.IOException;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class CopyFile {

	@Params("from, to, failIfExists")
	public static boolean CopyFile(Object a, Object b, Object c) {
		var from = Paths.get(Ops.toStr(a));
		var to = Paths.get(Ops.toStr(b));
		boolean failIfExists = Ops.toBoolean(c);

		try {
			if (failIfExists)
				Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES);
			else
				Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES,
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
