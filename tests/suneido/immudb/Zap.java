/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

/**
 * corrupt the end of the file
 * so it appears the database was not shut down properly
 */
public class Zap {

	public static void main(String[] args) {
		String filename = "suneido.dbi";
		MmapFile mmf = new MmapFile(filename, "rw");
		final int N = -200000; // negative for end relative
		ByteBuffer buf = mmf.rbuffer(N);
		buf.putLong(~0L);
		mmf.close();
		System.out.println("zapped " + filename + " at " + N);
	}

}
