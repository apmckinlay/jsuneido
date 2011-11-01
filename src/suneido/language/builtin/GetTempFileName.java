/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction2;

public class GetTempFileName extends SuFunction2 {
	{ params = new FunctionSpec("path", "prefix"); }

	@Override
	public Object call2(Object a, Object b) {
		String path = Ops.toStr(a);
		String prefix = Ops.toStr(b);
		if (prefix.length() < 3)
			prefix = (prefix + "___").substring(0, 3);
		try {
			File tmpfile = File.createTempFile(prefix, "", new File(path));
			tmpfile.deleteOnExit();
			return tmpfile.getPath().replace('\\', '/');
		} catch (IOException e) {
			throw new RuntimeException(
					"GetTempFileName(" + path + ", " + prefix + ")", e);
		}
	}

}
