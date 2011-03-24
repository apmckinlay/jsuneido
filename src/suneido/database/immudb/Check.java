/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.util.Checksum;

public class Check {
	private final int CKSUM_SIZE;
	private final Storage stor;
	private long pos = 0;

	Check(Storage stor) {
		this.stor = stor;
		CKSUM_SIZE = MmapFile.align(4);
	}

	// TODO will need length of good commits if check fails

	public boolean check() {
		Iterator<ByteBuffer> iter = stor.iterator(Storage.FIRST_ADR);
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
		int size = buf.getInt(buf.position()); // don't advance
		if (size == 0) { // chunk padding
			if (! iter.hasNext()) {
				buf.position(buf.limit());
				return buf;
			}
			pos += buf.remaining();
			buf = iter.next();
			size = buf.getInt(0); // don't advance
		}

		Checksum cksum = new Checksum();
		int n;
		for (int i = 0; i < size; i += n) {
			if (buf.remaining() == 0)
				buf = iter.next();
			n = Math.min(size - i, buf.remaining());
			cksum.update(buf, n);
		}
		if (buf.remaining() == 0)
			buf = iter.next();
		int stor_cksum = buf.getInt();
		buf.getInt(); // skip alignment
		if (stor_cksum != cksum.getValue())
			return null;
		pos += size += CKSUM_SIZE;
		return buf;
	}

	public long okSize() {
		return pos;
	}

}
