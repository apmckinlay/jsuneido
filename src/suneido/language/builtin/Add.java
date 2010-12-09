/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.SuFunction2;
import suneido.language.Ops;

public class Add extends SuFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.add(a, b);
	}

}
