/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.File;

import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction2;

public class SetFileWritable extends SuFunction2 {
	{ params = new FunctionSpec(array("file", "writable"), true); }

	@Override
	public Object call2(Object f, Object w) {
		File file = new File(Ops.toStr(f));
		boolean writable = Ops.toBoolean_(w);
		return file.setWritable(writable, false);
	}

}
