/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * Wraps a slice of a ByteBuffer.
 * Used for on-disk field values.
 * Avoids slicing or duplicating the ByteBuffer
 * Does not use or modify the ByteBuffer mutable data (position, etc.)
 */
@Immutable
public class DataBuf extends Data {
	private final ByteBuffer buf;
	private final int idx;
	private final int len;

	DataBuf(ByteBuffer buf, int idx, int len) {
		this.buf = buf;
		this.idx = idx;
		this.len = len;
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public void addTo(ByteBuffer dst) {
		for (int i = idx; i < idx + len; ++i)
			dst.put(buf.get(i));
	}

	@Override
	public byte[] asArray() {
		byte[] bytes = new byte[len];
		for (int i = 0; i < len; ++i)
			bytes[i] = buf.get(idx + i);
		return bytes;
	}

	@Override
	public byte byteAt(int i) {
		return buf.get(idx + i);
	}

}
