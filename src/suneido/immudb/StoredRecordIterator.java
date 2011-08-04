/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import com.google.common.collect.UnmodifiableIterator;

class StoredRecordIterator extends UnmodifiableIterator<Record> {
	private final Storage stor;
	private final int last;
	private int adr;

	StoredRecordIterator(Storage stor, int first, int last) {
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
		Record r = new Record(stor, adr);
		int len = r.storSize();
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

	int nextAdr() {
		return adr;
	}

}
