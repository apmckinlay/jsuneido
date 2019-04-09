/* Copyright 2018 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Suneido;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class LibraryOverride {

	@Params("lib, name, text = ''")
	public static Object LibraryOverride(Object a, Object b, Object c) {
		Suneido.context.override(Ops.toStr(a), Ops.toStr(b), Ops.toStr(c));
		return null;
	}

}
