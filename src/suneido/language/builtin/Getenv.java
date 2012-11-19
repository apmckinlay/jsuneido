/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction1;

public class Getenv extends SuFunction1 {
	{ params = FunctionSpec.string; }

	@Override
	public Object call1(Object a) {
		String name = Ops.toStr(a);
		return System.getenv(name);
	}

}
