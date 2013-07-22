/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;
import suneido.language.SuInstance;

public class ObjectQ {

	@Params("value")
	public static Boolean ObjectQ(Object a) {
		return a instanceof SuInstance ||
				null != Ops.toContainer(a);
	}

}
