/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import com.google.common.collect.UnmodifiableIterator;

public class StoredRecordIterator extends UnmodifiableIterator<Record> {
	private final Storage stor;
	private final int last;
	private int adr;

	public StoredRecordIterator(Storage stor, int first, int last) {
		this.stor = stor;
		this.last = last;
		adr = first;
	}

	@Override
	public boolean hasNext() {
		return adr <= last;
	}

	@Override
	public Record next() {
		assert hasNext();
		ByteBuffer buf = stor.buffer(adr);
		Record r = new Record(buf);
		int len = r.length();
		if (adr < last)
			adr = stor.advance(adr, skipPadding(buf, len));
		else
			adr = last + 1;
		return r;
	}

	private int skipPadding(ByteBuffer buf, int len) {
		int limit = buf.limit();
		while (len < limit && buf.get(len) == 0)
			++len;
		return len;
	}

	public int nextAdr() {
		return adr;
	}

}
