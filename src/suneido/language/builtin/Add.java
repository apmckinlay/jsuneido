/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.BuiltinFunction2;
import suneido.language.Ops;

public class Add extends BuiltinFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.add(a, b);
	}

}
