/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;

/**
 * Verify checksums and sizes.<p>
 * fastcheck is used at startup to confirm that database was closed ok<p>
 * fullcheck is used by {@link suneido.immudb.DbCheck}<p>
 */
class Check {
	private static final byte[] zero_tail = new byte[Tran.TAIL_SIZE];
	private static final int SIZEOF_INT = 4;
	private static final int FAST_NCOMMITS = 8;
	private static final int MIN_SIZE = Tran.HEAD_SIZE + Tran.TAIL_SIZE;
	private final Storage stor;
	private long pos = 0;
	private int nCommits = 0;
	private int lastOkDatetime = 0;

	Check(Storage stor) {
		this.stor = stor;
	}

	/** checks the last FAST_NCOMMITS commits */
	boolean fastcheck() {
		long fileSize = stor.sizeFrom(Storage.FIRST_ADR);
		int pos = 0; // negative offset from end of file
		int nCommits = 0;
		while (nCommits < FAST_NCOMMITS && fileSize + pos > MIN_SIZE) {
			ByteBuffer buf = stor.buffer(pos - SIZEOF_INT);
			int size = buf.getInt();
			if (! reasonableSize(size, fileSize + pos))
				return false;
			pos -= size;
			++nCommits;
		}
		if (nCommits == 0)
			return true; // empty file
		return checkFrom(pos);
	}

	private boolean reasonableSize(int size, long maxSize) {
		return MIN_SIZE <= size && size <= maxSize;
	}

	/** checks entire file */
	boolean fullcheck() {
		return checkFrom(Storage.FIRST_ADR);
	}

	private boolean checkFrom(int adr) {
		Iterator<ByteBuffer> iter = stor.iterator(adr);
		if (! iter.hasNext())
			return true; // empty
		ByteBuffer buf = iter.next();
		do
			if (null == (buf = check1(iter, buf)))
				return false;
		while (buf.remaining() > 0 || iter.hasNext());
		return true;
	}

	private ByteBuffer check1(Iterator<ByteBuffer> iter, ByteBuffer buf) {
		if (buf.remaining() == 0)
			buf = iter.next();
		Checksum cksum = new Checksum();
		int size = buf.getInt(buf.position()); // don't advance buf
		if (size == 0) { // chunk padding
			pos += buf.remaining();
			if (! iter.hasNext()) {
				buf.position(buf.limit());
				return buf;
			}
			cksum.update(buf); // padding is included in checksum
			buf = iter.next();
			size = buf.getInt(0); // don't advance buf
		}

		if (size < Tran.HEAD_SIZE + Tran.TAIL_SIZE)
			return null;

		int datetime = buf.getInt(buf.position() + SIZEOF_INT); // don't advance buf
		int notail_size = size - Tran.HEAD_SIZE;
		int n;
		for (int i = 0; i < notail_size; i += n) {
			if (buf.remaining() == 0)
				buf = iter.next();
			n = Math.min(notail_size - i, buf.remaining());
			cksum.update(buf, n);
		}
		cksum.update(zero_tail);
		if (buf.remaining() == 0)
			buf = iter.next();
		int stor_cksum = buf.getInt();
		int tail_size = buf.getInt();
		if (stor_cksum != cksum.getValue() || tail_size != size)
			return null;
		pos += size;
		++nCommits;
		lastOkDatetime = datetime;
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
