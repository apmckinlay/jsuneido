/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.util.FileUtils;

public class Check {
	private static final byte[] zero_tail = new byte[8];
	private final Storage stor;
	private long pos = 0;
	private int nCommits = 0;

	Check(Storage stor) {
		this.stor = stor;
	}

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
			pos += buf.remaining();
			if (! iter.hasNext()) {
				buf.position(buf.limit());
				return buf;
			}
			buf = iter.next();
			size = buf.getInt(0); // don't advance
		}

		if (size < Tran.HEAD_SIZE + Tran.TAIL_SIZE)
			return null;

		int notail_size = size - Tran.HEAD_SIZE;
		Checksum cksum = new Checksum();
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
		return buf;
	}

	public long okSize() {
		return pos;
	}

	public int nCommits() {
		return nCommits;
	}

	/** Fixing is easy - just copy the good prefix of the file */
	public static void fix(String filename, long okSize) {
		File tempfile = FileUtils.tempfile();
		try {
			FileUtils.copy(new File(filename), tempfile, okSize);
		} catch (IOException e) {
			throw new RuntimeException("fix copy failed", e);
		}
		FileUtils.renameWithBackup(tempfile, filename);
	}

}
