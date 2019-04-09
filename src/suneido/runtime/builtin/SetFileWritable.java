/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class SetFileWritable {

	@Params("file, writable = true")
	public static Boolean SetFileWritable(Object f, Object w) {
		File file = new File(Ops.toStr(f));
		boolean writable = Ops.toBoolean_(w);
		return file.setWritable(writable, false);
	}

}
