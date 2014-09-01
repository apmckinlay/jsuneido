/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.MoreObjects;

/**
 * Check database integrity.<p>
 * Used by {@link DbCheck} and {@link DbRebuild}.<p>
 * {@link Database}.open uses fastcheck which only checks the end part of the database.
 */
class Check {
	private static final int FAST_NPERSISTS = 5;
	private static final int EMPTY = -2;
	private static final int CORRUPT = -1;
	private final Storage dstor;
	private final Storage istor;
	/** set by findLast for fastcheck */
	private int lastadr = 0;
	private Date lastOkDate = null;
	StorageIter dIter;
	private StorageIter iIter;
	private long dOkSize = 0;
	private long iOkSize = 0;

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

	/**
	 * Checks entire database. Used by DbCheck and DbRebuild.
	 * Verifies checksums and confirms that data and index files match.
	 * @return true if the entire database appears valid
	 */
	boolean fullcheck() {
		return checkFrom(Storage.FIRST_ADR, Storage.FIRST_ADR);
	}

	/** check the last FAST_NPERSISTS persists */
	boolean fastcheck() {
		try {
			int adr = findLast(FAST_NPERSISTS);
			return (adr == CORRUPT) ? false
					: (adr == EMPTY) ? true : checkFrom(lastadr, adr);
		} catch (RuntimeException e) {
			System.out.println(e);
			return false;
		}
	}

	/** Scan backwards to find the n'th last persist */
	private int findLast(int nPersists) {
		StorageIterReverse iter = new StorageIterReverse(istor);
		int adr = 0;
		for (int n = 0; n < nPersists && iter.hasPrev(); ++n)
			adr = iter.prev();
		if (adr == 0)
			return EMPTY;
		long size = Storage.intToSize(istor.buffer(adr).getInt());
		lastadr = info(istor, adr, size).lastadr;
		return adr;
	}

	static PersistInfo info(Storage istor, int adr, long size) {
		adr = istor.advance(adr, size - Persist.ENDING_SIZE - Persist.TAIL_SIZE);
		ByteBuffer buf = istor.buffer(adr);
		int dbinfoadr = buf.getInt();
		int maxtblnum = buf.getInt();
		int lastcksum = buf.getInt();
		int lastadr = buf.getInt();
		return new PersistInfo(dbinfoadr, maxtblnum, lastcksum, lastadr);
	}

	@Immutable
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
			return MoreObjects.toStringHelper(this)
					.add("dbinfoadr", Storage.adrToOffset(dbinfoadr))
					.add("maxtblnum", maxtblnum)
					.add("lastadr", Storage.adrToOffset(lastadr))
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
		while (dIter.notFinished() && iIter.notFinished()) {
			if (iInfo == null)
				iInfo = info(istor, iIter.adr(), iIter.size());
			if (iInfo.lastcksum == dIter.cksum() && iInfo.lastadr == dIter.adr()) {
				dOkSize = dIter.sizeInc();
				iOkSize = iIter.sizeInc();
				if (dIter.date() != null)
					lastOkDate = dIter.date();
				iIter.advance();
				iInfo = null;
				if (iIter.eof())
					dIter.advance();
			} else if (dIter.adr() < iInfo.lastadr)
				dIter.advance();
			else // diter has gone past iIter with no match - no point continuing
				break;
		}
		return dIter.eof() && iIter.eof();  // matched all the way to the end
	}

	/** The date/time of the last data commit where data and indexes matched */
	Date lastOkDate() {
		return lastOkDate;
	}

	/** The size of data at the last data and indexes match */
	long dOkSize() {
		return dOkSize;
	}

	/** The size of indexes at the last data and indexes match */
	long iOkSize() {
		return iOkSize;
	}

	public static void main(String[] args) {
		String dbfilename = "suneido.db";
		System.out.println("Fastcheck " + dbfilename + " " +
				(Check.fastcheck(dbfilename) ? "succeeded" : "FAILED"));
	}

}
