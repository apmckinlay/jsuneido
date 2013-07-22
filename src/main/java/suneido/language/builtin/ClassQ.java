/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Params;
import suneido.language.SuClass;

public class ClassQ {

	@Params("value")
	public static boolean ClassQ(Object a) {
		return a instanceof SuClass;
	}

}
