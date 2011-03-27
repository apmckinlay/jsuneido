/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.util.Checksum;
import suneido.util.FileUtils;

public class Check {
	private static final byte[] zero_tail = new byte[8];
	private final Storage stor;
	private long pos = 0;

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
		for (int i = 0; i < size - Tran.HEAD_SIZE; i += n) {
			if (buf.remaining() == 0)
				buf = iter.next();
			n = Math.min(size - i, buf.remaining());
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
		return buf;
	}

	public long okSize() {
		return pos;
	}

	public void fix(String filename) throws IOException {
		MmapFile mmf = new MmapFile(filename, "r");
		Check check = new Check(mmf);
		mmf.close();
		if (check.check())
			return;
		File tempfile = FileUtils.tempfile();
		FileUtils.copy(new File(filename), tempfile, check.okSize());
		FileUtils.renameWithBackup(tempfile, filename);
	}

}
