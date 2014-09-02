/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.runtime.SuInstance;

public class ObjectQ {

	@Params("value")
	public static Boolean ObjectQ(Object a) {
		return a instanceof SuInstance ||
				null != Ops.toContainer(a);
	}

}
