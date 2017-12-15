/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuDate;

@ThreadSafe
public class Timestamp {

	private volatile static SuDate prev = SuDate.now();
	// not sure why this needs to be volatile when using synchronized
	// but got duplicate timestamps without it ???

	public static synchronized SuDate next() {
		SuDate ts = SuDate.now();
		if (ts.compareTo(prev) <= 0)
			ts = prev.plus(0, 0, 0, 0, 0, 0, 1);
		prev = ts;
		return ts;
	}

}
