/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class GetTempFileName {

	@Params("path, prefix")
	public static String GetTempFileName(Object a, Object b) {
		String path = Ops.toStr(a);
		String prefix = Ops.toStr(b);
		if (prefix.length() < 3)
			prefix = (prefix + "___").substring(0, 3);
		try {
			File tmpfile = File.createTempFile(prefix, "", new File(path));
			tmpfile.deleteOnExit();
			return tmpfile.getPath().replace('\\', '/');
		} catch (IOException e) {
			throw new SuException(
					"GetTempFileName(" + path + ", " + prefix + ")", e);
		}
	}

}
