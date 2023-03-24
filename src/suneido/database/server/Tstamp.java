/* Copyright 2023 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.util.concurrent.TimeUnit;

import suneido.SuDate;
import suneido.SuTimestamp;
import suneido.Suneido;
import suneido.util.ThreadSafe;

// This is the client side of timestamps.
// It handles issuing from the batches.
@ThreadSafe
public class Tstamp {
	static int limit = 0;
	static int count = 0;
	static SuDate last;

	public static synchronized SuDate next(Dbms dbms) {
		count++;
		if (count < limit) {
			// fast path
			if (limit == Timestamp.initialBatch) {
				last = last.plus(0, 0, 0, 0, 0, 0, 1);
				return last;
			}
			return new SuTimestamp(last, (byte) count);
		}
		if (limit == 0)
			Suneido.scheduleAtFixedRate(Tstamp::expire, 1, TimeUnit.SECONDS);
		// fetch a new timestamp, slow path
		last = dbms.timestamp();
		count = 0;
		if (last.millisecond() < Timestamp.threshold)
			limit = Timestamp.initialBatch;
		else
			limit = 256;
		return last;
	}

	private static synchronized void expire() {
		count = limit + 1;
	}

}
