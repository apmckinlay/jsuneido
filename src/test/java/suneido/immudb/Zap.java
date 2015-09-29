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
		try (MmapFile mmf = new MmapFile(filename, "rw")) {
			long offset = -56; // negative for end relative
			ByteBuffer buf = offset < 0
					? mmf.rbuffer(offset)
					: mmf.buffer(Storage.offsetToAdr(offset));
			buf.putLong(~0L);
			System.out.println("zapped " + filename + " at " + offset);
		}
	}

}
