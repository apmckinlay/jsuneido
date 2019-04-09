/* Copyright 2019 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.runtime.Params;
import suneido.runtime.SuInstance;

public class InstanceQ {

	@Params("value")
	public static Boolean InstanceQ(Object a) {
		return a instanceof SuInstance;
	}

}
