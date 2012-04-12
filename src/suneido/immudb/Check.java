/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;

import suneido.immudb.StorageIter.Status;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

class Check {
	private static final int FAST_NPERSISTS = 4;
	private static final int MIN_SIZE = Tran.HEAD_SIZE + Tran.TAIL_SIZE;
	private static final int EMPTY = -2;
	private static final int CORRUPT = -1;
	private final Storage dstor;
	private final Storage istor;
	/** set by findLast for fastcheck */
	private int lastadr = 0;
	private Date lastOkDate = null;
	StorageIter dIter;
	StorageIter iIter;

	/**
	 * Used when opening a database to quickly check it,
	 * primarily to confirm it was closed properly.
	 */
	static boolean fastcheck(String dbFilename) {
		Storage dstor = new MmapFile(dbFilename + "d", "r");
		Storage istor = new MmapFile(dbFilename + "i", "r");
		try {
			return new Check(dstor, istor).fastcheck();
		} finally {
			dstor.close();
			istor.close();
		}
	}

	Check(Storage dstor, Storage istor) {
		this.dstor = dstor;
		this.istor = istor;
	}

	/** Checks entire database. Used by DbCheck and DbRebuild */
	boolean fullcheck() {
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
		lastadr = info(istor, pos, size).lastadr;
		return pos;
	}

	private static boolean isValidSize(Storage stor, long pos, int size) {
		return MIN_SIZE <= size && stor.isValidPos(pos - size);
	}

	/** Only works on index store */
	static PersistInfo info(Storage stor, int adr, int size) {
		ByteBuffer buf = stor.buffer(stor.advance(adr,
				size - Persist.ENDING_SIZE - Persist.TAIL_SIZE));
		int dbinfoadr = buf.getInt();
		int maxtblnum = buf.getInt();
		int lastcksum = buf.getInt();
		int lastadr = buf.getInt();
		return new PersistInfo(dbinfoadr, maxtblnum, lastcksum, lastadr);
	}

	static class PersistInfo {
		final int dbinfoadr;
		final int maxtblnum;
		final int lastadr;
		final int lastcksum;

		public PersistInfo(int dbinfoadr, int maxtblnum, int lastcksum, int lastadr) {
			this.dbinfoadr = dbinfoadr;
			this.maxtblnum = maxtblnum;
			this.lastcksum = lastcksum;
			this.lastadr = lastadr;
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.add("dbinfoadr", dbinfoadr)
					.add("maxtblnum", maxtblnum)
					.add("lastadr", lastadr)
					.add("lastcksum", Integer.toHexString(lastcksum))
					.toString();
		}
	}

	/**
	 * Check dstor and istor in parallel.
	 * @return true if no problems found
	 */
	private boolean checkFrom(int dAdr, int iAdr) {
		dIter = new StorageIter(dstor, dAdr);
		iIter = new StorageIter(istor, iAdr);
		PersistInfo iInfo = null;
		while (! dIter.eof() && ! iIter.eof()) {
			if (iInfo == null)
				iInfo = Check.info(istor, iIter.adr, iIter.size);
			if (iInfo.lastcksum == dIter.cksum() && iInfo.lastadr == dIter.adr) {
				iIter.advance();
				iInfo = null;
				if (iIter.eof())
					dIter.advance();
			} else if (dIter.adr() < iInfo.lastadr)
				dIter.advance();
			else // diter has gone past iIter with no match - no point continuing
				break;
			if (dIter.status != Status.OK || iIter.status != Status.OK)
				break;
			if (dIter.date() != null)
				lastOkDate = dIter.date();
		}
		return dIter.eof() && iIter.eof();  // matched all the way to the end
	}

	Date lastOkDate() {
		return lastOkDate;
	}

}
