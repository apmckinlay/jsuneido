/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import java.util.List;

import suneido.compiler.ClassGen;
import suneido.runtime.BlockReturnException;
import suneido.runtime.Ops;
import suneido.runtime.SuFunction;

public class SampleFunction extends SuFunction {
	private static final Object c;

	static {
		List<Object> constants = ClassGen.shareConstants.get();
		c = constants.get(0);
	}

	@Override
	public Object call(Object... args) {
		try {
			throw Ops.blockReturnException(c, 123);
		} catch (BlockReturnException e) {
			return Ops.blockReturnHandler(e, 123);
		}
	}

}