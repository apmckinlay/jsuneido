/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuContainer;
import suneido.runtime.Builtins;

public class BuiltinNames {

	public static SuContainer BuiltinNames() {
		return new SuContainer(Builtins.builtinNames());
	}

}