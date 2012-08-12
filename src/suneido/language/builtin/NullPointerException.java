/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.language.SuFunction0;

public class NullPointerException extends SuFunction0 {
	public static Object x;

	@Override
	public Object call0() {
		return x.toString();
	}

}
