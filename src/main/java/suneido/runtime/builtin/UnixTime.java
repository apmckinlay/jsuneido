/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Date;

import suneido.util.Dnum;

public class UnixTime {

	public static Object UnixTime() {
		return Dnum.from(new Date().getTime() / 1000);
	}

}
