/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.collect.AbstractIterator;

class TestStorage implements Storage {
	private final List<ByteBuffer> data = new ArrayList<ByteBuffer>();

	@Override
	public int alloc(int size) {
		size = MmapFile.align(size);
		ByteBuffer buf = ByteBuffer.allocate(size);
		data.add(buf);
		return data.size(); // +1 to avoid 0
	}

	@Override
	public ByteBuffer buffer(int adr) {
		if (adr < 0) {
			// only handles accessing last allocation
			assert -adr == data.get(adr - 1).capacity();
			adr = data.size();
		}
		return buf(adr - 1);
	}

	public ByteBuffer buf(int adr) {
		ByteBuffer buf = data.get(adr);
		buf.position(0);
		return buf;
	}

	@Override
	public Iterator<ByteBuffer> iterator(int adr) {
		return new Iter(adr);
	}

	private class Iter extends AbstractIterator<ByteBuffer> {
		private int pos;

		public Iter(int adr) {
			pos = adr - 1;
		}

		@Override
		protected ByteBuffer computeNext() {
			if (pos < data.size())
				return buf(pos++);
			return endOfData();
		}

	}

	@Override
	public int sizeFrom(int adr) {
		int size = 0;
		for (int i = adr - 1; i < data.size(); ++i)
			size += data.get(i).limit();
		return size;
	}

}