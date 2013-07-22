/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Params;
import suneido.language.SuCallable;

public class FunctionQ {

	@Params("value")
	public static Boolean FunctionQ(Object a) {
		return a instanceof SuCallable;
	}

}
