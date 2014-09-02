/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.WhenBuilt;

public class Built {

	public static String Built() {
		return WhenBuilt.when();
	}

}