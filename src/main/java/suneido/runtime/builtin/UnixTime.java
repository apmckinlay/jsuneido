/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Date;

public class UnixTime {

	public static long UnixTime() {
		return new Date().getTime() / 1000;
	}

}
