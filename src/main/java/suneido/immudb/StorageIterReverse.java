/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

//TODO change to use adr instead of pos (like HistoryIterator)

/**
 * Minimal reverse iterator.
 * @see StorageIter
 */
public class StorageIterReverse {
	private static long MIN_SIZE;
	private final Storage stor;
	private final long fileSize;
	private long rpos = 0; // <= 0

	StorageIterReverse(Storage stor) {
		this.stor = stor;
		fileSize = stor.sizeFrom(0);
		MIN_SIZE = Storage.adrToOffset(Storage.FIRST_ADR) + Tran.HEAD_SIZE + Tran.TAIL_SIZE;
	}

	boolean hasPrev() {
		return fileSize + rpos > MIN_SIZE;
	}

	int prev() {
		// skip zero end of chunk padding
		long size;
		while (true) {
			size = Storage.intToSize(stor.rbuffer(rpos - Integer.BYTES).getInt());
			if (size != 0)
				break;
			rpos -= Integer.BYTES;
		}
		if (! isValidSize(stor, rpos, size))
			throw new RuntimeException("bad size " + size);
		rpos -= size;
		return stor.rposToAdr(rpos);
	}

	private static boolean isValidSize(Storage stor, long pos, long size) {
		return MIN_SIZE <= size && stor.isValidPos(pos - size);
	}

}
