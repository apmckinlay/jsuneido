/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuObject;
import suneido.runtime.Builtins;

public class BuiltinNames {

	public static SuObject BuiltinNames() {
		return new SuObject(Builtins.builtinNames());
	}

}