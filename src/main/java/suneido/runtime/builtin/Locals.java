/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.List;

import suneido.SuException;
import suneido.debug.DebugManager;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Locals {

	@Params("offset")
	public static Object Locals(Object a) {
		int offset = 0;
		try {
			offset = Ops.toInt(a);
		} catch (SuException e) {
			return Boolean.FALSE;
		}
		List<suneido.debug.Frame> f = DebugManager.getInstance()
				.makeCallstackForCurrentThread(new Throwable()).frames();
		if (0 <= offset && offset < f.size()) {
			return f.get(offset).getLocalsContainer();
		} else {
			return Boolean.FALSE;
		}
	}
}
