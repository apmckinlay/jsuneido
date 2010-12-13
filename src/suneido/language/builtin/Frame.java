/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.BuiltinFunction1;
import suneido.language.FunctionSpec;

public class Frame extends BuiltinFunction1 {
	{ params = new FunctionSpec("offset"); }

	@Override
	public Object call1(Object a) {
		return Boolean.FALSE; // TODO Frame
	}

}
