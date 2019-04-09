/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;
import suneido.runtime.SuCallable;

public class FunctionQ {

	@Params("value")
	public static Boolean FunctionQ(Object a) {
		return a instanceof SuCallable;
	}

}
