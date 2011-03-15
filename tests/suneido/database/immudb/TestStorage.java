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
	public ByteBuffer buffer(int adr) {
		if (adr < 0) {
			// only handles accessing last allocation
			assert -adr == data.get(adr - 1).capacity();
			adr = data.size();
		}
		return data.get(adr - 1);
	}

	@Override
	public int alloc(int size) {
		ByteBuffer buf = ByteBuffer.allocate(size);
		data.add(buf);
		return data.size(); // +1 to avoid 0
	}

	@Override
	public Iterator<ByteBuffer> iterator() {
		return new Iter();
	}

	private class Iter extends AbstractIterator<ByteBuffer> {
		private int pos = data.size();

		@Override
		protected ByteBuffer computeNext() {
			if (pos < data.size())
				return data.get(pos++);
			return endOfData();
		}

	}

}