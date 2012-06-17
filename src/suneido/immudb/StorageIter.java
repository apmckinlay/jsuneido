/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;

import com.google.common.base.Objects;

/**
 * Iterate through data commits or index persists.
 * Check sizes and checksums.
 * CommitProcessor handles structure within commit.
 */
class StorageIter {
	enum Status { OK, SIZE_MISMATCH, CHECKSUM_FAIL };
	private static final byte[] zero_tail = new byte[Tran.TAIL_SIZE];
	final Storage stor;
	/** address (not offset) of current commit/persist */
	private int adr;
	/** size of current commit/persist */
	private int size;
	/** date/time of current commit/persist */
	private int date = 0;
	private Status status = Status.OK;
	private int cksum; // of current commit/persist

	StorageIter(Storage stor) {
		this(stor, Storage.FIRST_ADR);
	}

	StorageIter(Storage stor, int adr) {
		this.stor = stor;
		seek(adr);
	}

	void seek(int adr) {
		this.adr = adr;
		if (eof())
			return ;
		ByteBuffer buf = stor.buffer(adr);
		size = buf.getInt();
		date = buf.getInt();
		ByteBuffer endbuf = stor.buffer(stor.advance(adr, size - Tran.TAIL_SIZE));
		cksum = endbuf.getInt();
		int endsize = endbuf.getInt();
		if (endsize != size) {
			status = Status.SIZE_MISMATCH;
			return;
		}
		if (date == 0) // aborted commit
			return;
		if (! verifyCksum()) {
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

	int size() {
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
	private boolean verifyCksum() {
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
		return cksum == cs.getValue();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("adr", adr)
				.add("eof", eof())
				.toString();
	}

	public boolean notFinished() {
		return ! eof() && status == Status.OK;
	}

}
