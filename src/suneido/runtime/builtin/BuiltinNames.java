/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuObject;
import suneido.runtime.Builtins;

public class BuiltinNames {
	private static final SuObject list = getList();

	public static SuObject BuiltinNames() {
		return list;
	}

	private static SuObject getList() {
		var ob = new SuObject(Builtins.builtinNames());
		ob.sort(Boolean.FALSE);
		return ob.setReadonly();
	}
}