/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;

import com.google.common.primitives.Ints;

/**
 * Verify checksums and sizes.<p>
 * fastcheck is used at startup to confirm that database was closed ok<p>
 * fullcheck is used by {@link suneido.immudb.DbCheck}<p>
 */
class Check {
	private static final int EMPTY = -2;
	private static final int CORRUPT = -1;
	private static final byte[] zero_tail = new byte[Tran.TAIL_SIZE];
	private static final int FAST_NCOMMITS = 8;
	private static final int MIN_SIZE = Tran.HEAD_SIZE + Tran.TAIL_SIZE;
	private final Storage stor;
	private long pos = 0;
	private int nCommits = 0;
	private int lastOkDatetime = 0;

	Check(Storage stor) {
		this.stor = stor;
	}

	/** check the last FAST_NCOMMITS commits */
	boolean fastcheck() {
		int pos = findLast(FAST_NCOMMITS);
		return (pos == CORRUPT) ? false : (pos == EMPTY) ? true : checkFrom(pos);
	}

	/**
	 * Read backwards from the end nCommits commits
	 * (or to the beginning if there are fewer total commits).
	 * @return The address of the commit.
	 */
	private int findLast(int nCommits) {
		long fileSize = stor.sizeFrom(Storage.FIRST_ADR);
		int pos = 0; // negative offset from end of file
		int n = 0;
		while (n < nCommits && fileSize + pos > MIN_SIZE) {
			ByteBuffer buf = stor.buffer(pos - Ints.BYTES);
			int size = buf.getInt();
			if (! isValidSize(pos, size))
				return CORRUPT;
			pos -= size;
			++n;
		}
		if (n == 0)
			return EMPTY; // empty file
		return pos;
	}

	private boolean isValidSize(long pos, int size) {
		return MIN_SIZE <= size && stor.isValidPos(pos - size);
	}

	/** checks entire file */
	boolean fullcheck() {
		//PERF check concurrently forwards from beginning and backwards from end
		return checkFrom(Storage.FIRST_ADR);
	}

	private boolean checkFrom(int adr) {
		Iterator<ByteBuffer> iter = stor.iterator(adr);
		if (! iter.hasNext())
			return true; // empty
		ByteBuffer buf = iter.next();
		do {
			if (null == (buf = check1(iter, buf)))
				return false;
		} while (buf.remaining() > 0 || iter.hasNext());
		return true;
	}

	private ByteBuffer check1(Iterator<ByteBuffer> iter, ByteBuffer buf) {
		buf = nextChunk(iter, buf);
		if (buf == null)
			return null;
		Checksum cksum = new Checksum();
		int size = buf.getInt(buf.position()); // don't advance buf
		if (! isValidSize(pos, size))
			return null;
		int datetime = buf.getInt(buf.position() + Ints.BYTES); // don't advance buf
		int notail_size = size - Tran.HEAD_SIZE;
		int n;
		for (int i = 0; i < notail_size; i += n) {
			buf = nextChunk(iter, buf);
			if (buf == null)
				return null;
			n = Math.min(notail_size - i, buf.remaining());
			cksum.update(buf, n);
		}
		cksum.update(zero_tail);
		buf = nextChunk(iter, buf);
		if (buf == null)
			return null;
		int stor_cksum = buf.getInt();
		int tail_size = buf.getInt();
		if (stor_cksum != cksum.getValue() || tail_size != size)
			return null;
		pos += size;
		++nCommits;
		lastOkDatetime = datetime;
		return buf;
	}

	private static ByteBuffer nextChunk(Iterator<ByteBuffer> iter, ByteBuffer buf) {
		if (buf.remaining() == 0) {
			if (! iter.hasNext())
				return null;
			buf = iter.next();
		}
		return buf;
	}

	long okSize() {
		return pos;
	}

	int nCommits() {
		return nCommits;
	}

	Date lastOkDatetime() {
		return new Date(1000L * lastOkDatetime);
	}

}
