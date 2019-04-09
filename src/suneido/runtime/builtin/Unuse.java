/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Suneido;
import suneido.TheDbms;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Unuse {

	@Params("library")
	public static Boolean Unuse(Object a) {
		if (! TheDbms.dbms().unuse(Ops.toStr(a)))
			return false;
		Suneido.context.clearAll();
		return true;
	}

}
