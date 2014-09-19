/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuContainer;
import suneido.runtime.Args;
import suneido.runtime.BuiltinClass;

public class ObjectClass extends BuiltinClass {

	public ObjectClass() {
		super("Object", null);
	}

	@Override
	public Object newInstance(Object... args) {
		return create(args);
	}

	/** used by direct calls in generated code */
	public static Object create(Object... args) {
		return Args.collectArgs(new SuContainer(), args);
	}

}
