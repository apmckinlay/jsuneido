/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.BuiltinClass;

public class RecordClass extends BuiltinClass {

	@Override
	public Object newInstance(Object... args) {
		return create(args);
	}

	/** used by direct calls in generated code */
	public static Object create(Object... args) {
		return Args.collectArgs(new SuRecord(), args);
	}

}
