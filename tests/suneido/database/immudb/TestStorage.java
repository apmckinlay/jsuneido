/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class TestStorage implements Storage {
	private final List<ByteBuffer> data = new ArrayList<ByteBuffer>();

	@Override
	public ByteBuffer buffer(int adr) {
		return data.get(adr - 1);
	}

	@Override
	public int alloc(int size) {
		ByteBuffer buf = ByteBuffer.allocate(size);
		data.add(buf);
		return data.size(); // +1 to avoid 0
	}

}