/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;

import com.google.common.base.MoreObjects;

/**
 * Iterate through data commits or index persists.
 * Check sizes and checksums.
 * CommitProcessor handles structure within commit.
 * <p>
 * Used by {@link Check} and {@link DbRebuild}
 * @see StorageIterReverse
 */
class StorageIter {
	enum Status { OK, SIZE_MISMATCH, CHECKSUM_FAIL, BAD_SIZE };
	private static final byte[] zero_tail = new byte[Tran.TAIL_SIZE];
	final Storage stor;
	/** address (not offset) of current commit/persist */
	private int adr;
	/** size of current commit/persist */
	private long size;
	/** date/time of current commit/persist */
	private int date = 0;
	private Status status = Status.OK;
	private int cksum; // of current commit/persist
	private boolean verifyChecksums = true;

	StorageIter(Storage stor) {
		this(stor, Storage.FIRST_ADR);
	}

	StorageIter(Storage stor, int adr) {
		this.stor = stor;
		seek(adr);
	}

	/** used by dump */
	StorageIter dontChecksum() {
		verifyChecksums = false;
		return this;
	}

	private void seek(int adr) {
		this.adr = adr;
		if (eof())
			return ;
		ByteBuffer buf = stor.buffer(adr);
		size = Storage.intToSize(buf.getInt());
		date = buf.getInt();
		int end = stor.advance(adr, size - Tran.TAIL_SIZE);
		if (! stor.isValidAdr(end)) {
			status = Status.BAD_SIZE;
			return;
		}
		ByteBuffer endbuf = stor.buffer(end);
		cksum = endbuf.getInt();
		long endsize = Storage.intToSize(endbuf.getInt());
		if (endsize != size) {
			status = Status.SIZE_MISMATCH;
			return;
		}
		if (date == 0) // aborted commit
			return;
		if (verifyChecksums && ! verifyChecksum()) {
			status = Status.CHECKSUM_FAIL;
			return;
		}
	}

	boolean eof() {
		return stor.sizeFrom(adr) <= 0;
	}

	/** skips aborted commits */
	void advance() {
		do
			seek(stor.advance(adr, size));
		while (date == 0 && ! eof());
	}

	void advance2() {
		seek(stor.advance(adr, size));
	}

	Status status() {
		return status;
	}

	int adr() {
		return adr;
	}

	long size() {
		return size;
	}

	/** size of file up to and including the current commit/persist */
	long sizeInc() {
		return Storage.adrToOffset(adr) + size;
	}

	/** @return null for aborted commit */
	Date date() {
		return date == 0 ? null : new Date(1000L * date);
	}

	int cksum() {
		return cksum;
	}

	// depends on buf.remaining() going to end of storage chunk
	private boolean verifyChecksum() {
		Checksum cs = new Checksum();
		long remaining = size - Tran.HEAD_SIZE;
		int pos = adr;
		while (remaining > 0) {
			ByteBuffer buf = stor.buffer(pos);
			// safe as long as chunk size < Integer.MAX_VALUE
			int n = (int) Math.min(buf.remaining(), remaining);
			cs.update(buf, n);
			remaining -= n;
			pos = stor.advance(pos, n);
		}
		cs.update(zero_tail);
		return cksum == cs.getValue();
	}

	public boolean notFinished() {
		return ! eof() && status == Status.OK;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("adr", adr)
				.add("eof", eof())
				.toString();
	}

}
