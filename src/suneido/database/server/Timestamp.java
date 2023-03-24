/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import suneido.util.ThreadSafe;

import suneido.SuDate;

// This is the server side of timestamps
@ThreadSafe
public class Timestamp {
	// these must match gSuneido
	final static int initialBatch = 5;
	final static int threshold = 500;

	private static SuDate timestamp = SuDate.now().withoutMs();

	public static synchronized SuDate next() {
		var ts = timestamp;
		if (ts.millisecond() < threshold)
			timestamp = timestamp.plus(0, 0, 0, 0, 0, 0, initialBatch);
		else
			timestamp = timestamp.plus(0, 0, 0, 0, 0, 0, 1);
		return ts;
	}

	// sync is scheduled by Suneido.openDbms to run once per second
	public static synchronized void sync() {
		var ts = SuDate.now().withoutMs();
		if (ts.compareTo(timestamp) > 0)
			timestamp = ts;
	}

}
