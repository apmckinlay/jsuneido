/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;

import com.google.common.primitives.Ints;

class Check2 {
	private static final int FAST_NPERSISTS = 2;
	private static final int MIN_SIZE = Tran.HEAD_SIZE + Tran.TAIL_SIZE;
	private static final int EMPTY = -2;
	private static final int CORRUPT = -1;
	private final Storage dstor;
	private final Storage istor;
	/** set by findLast for fastcheck */
	private int lastadr = 0;

	Check2(Storage dstor, Storage istor) {
		this.dstor = dstor;
		this.istor = istor;
	}

	/** checks entire file */
	boolean fullcheck() {
		//PERF check concurrently forwards from beginning and backwards from end
		return checkFrom(Storage.FIRST_ADR, Storage.FIRST_ADR);
	}

	/** check the last FAST_NPERSISTS persists */
	boolean fastcheck() {
		int pos = findLast(FAST_NPERSISTS);
		return (pos == CORRUPT) ? false : (pos == EMPTY) ? true : checkFrom(lastadr, pos);
	}

	/** scan backwards to find the n'th last persist */
	private int findLast(int nPersists) {
		long fileSize = istor.sizeFrom(0);
		int pos = 0; // negative offset from end of file
		int n = 0;
		int size = 0;
		while (n < nPersists && fileSize + pos > MIN_SIZE) {
			ByteBuffer buf = istor.buffer(pos - Ints.BYTES);
			size = buf.getInt();
			if (! isValidSize(istor, pos, size))
				return CORRUPT;
			pos -= size;
			++n;
		}
		if (n == 0)
			return EMPTY; // empty file
		lastadr = info(istor, pos, size).adr;
		return pos;
	}

	private static boolean isValidSize(Storage stor, long pos, int size) {
		return MIN_SIZE <= size && stor.isValidPos(pos - size);
	}

	/** Only works on index store */
	private static Tran.StoreInfo info(Storage stor, int adr, int size) {
		ByteBuffer buf = stor.buffer(stor.advance(adr,
				size - Persist.ENDING_SIZE - Persist.TAIL_SIZE));
		buf.getInt(); // skip dbinfo adr
		int lastcksum = buf.getInt();
		int lastadr = buf.getInt();
		return new Tran.StoreInfo(lastcksum, lastadr);
	}

	/** Check dstor and istor in parallel */
	private boolean checkFrom(int dAdr, int iAdr) {
		Check2.Iter dIter = new Check2.Iter(dstor, dAdr);
		Check2.Iter iIter = new Check2.Iter(istor, iAdr);
		Tran.StoreInfo iInfo = null;
		while (! dIter.eof() && ! iIter.eof()) {
			if (iInfo == null)
				iInfo = iIter.info();
			if (iInfo.cksum == dIter.cksum()) {
				dIter.advance();
				iIter.advance();
				iInfo = null;
			} else if (dIter.adr() < iInfo.adr)
				dIter.advance();
			else
				return false;
		}
		if (! dIter.eof() || ! iIter.eof())
			return false;
		return true;
	}

	/**
	 * Iterate through data commits or index persists.
	 * Check sizes and checksums.
	 */
	static class Iter {
		private static final byte[] zero_tail = new byte[Tran.TAIL_SIZE];
		final Storage stor;
		private int adr; // of current commit/persist
		private int size; // of current commit/persist
		private int cksum; // of current commit/persist
		private long okSize = 0;
		private int lastOkDatetime = 0;

		Iter(Storage stor, int adr) {
			this.stor = stor;
			seek(adr);
		}

		void seek(int adr) {
			this.adr = adr;
			if (eof())
				return ;
			ByteBuffer buf = stor.buffer(adr);
			size = buf.getInt();
			ByteBuffer endbuf = stor.buffer(stor.advance(adr, size - Tran.TAIL_SIZE));
			cksum = endbuf.getInt();
			verifyCksum();
			int endsize = endbuf.getInt();
			if (endsize != size)
				throw new RuntimeException("storage size mismatch");
			okSize = ChunkedStorage.adrToOffset(stor.advance(adr, size));
			lastOkDatetime = buf.getInt();
		}

		boolean eof() {
			return stor.sizeFrom(adr) == 0;
		}

		void advance() {
			seek(stor.advance(adr, size));
		}

		int adr() {
			return adr;
		}

		int cksum() {
			return cksum;
		}

		long okSize() {
			return okSize;
		}

		Date lastOkDatetime() {
			return new Date(1000L * lastOkDatetime);
		}

		/** Only works on index store */
		Tran.StoreInfo info() {
			return Check2.info(stor, adr, size);
		}

		// need to handle spanning multiple storage chunks
		// depends on buf.remaining() going to end of chunk
		private void verifyCksum() {
			Checksum cs = new Checksum();
			int remaining = size - Tran.HEAD_SIZE;
			int pos = adr;
			while (remaining > 0) {
				ByteBuffer buf = stor.buffer(pos);
				int n = Math.min(buf.remaining(), remaining);
				cs.update(buf, n);
				remaining -= n;
				pos = stor.advance(pos, n);
			}
			cs.update(zero_tail);
			if (cksum != cs.getValue())
				throw new RuntimeException("invalid checksum");
		}

		@Override
		public String toString() {
			return "Iter(adr " + adr + (eof() ? " eof" : "") + ")";
		}

	} // end of Iter

}
