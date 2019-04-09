/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;
import suneido.runtime.SuClass;

public class ClassQ {

	@Params("value")
	public static boolean ClassQ(Object a) {
		return a instanceof SuClass;
	}

}
