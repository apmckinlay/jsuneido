/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

/**
 * useful to corrupt the database to test check & rebuild
 */
public class Zap {

	public static void main(String[] args) {
		String filename = "suneido.dbi";
		try (MmapFile mmf = new MmapFile(filename, "rw")) {
			// negative for end relative
			//long offset = 56; // will make dbi unusable 
			//long offset = -56; // quick rebuild, detected by fast check
			//long offset = -500000; // not detected by fast check, quick rebuild
			long offset = 500000; // not detected by fast check, slower rebuild
			ByteBuffer buf = offset < 0
					? mmf.rbuffer(offset)
					: mmf.buffer(Storage.offsetToAdr(offset));
			buf.putLong(~0L);
			System.out.println("zapped " + filename + " at " + offset);
		}
	}

}
