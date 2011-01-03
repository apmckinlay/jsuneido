/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * Wraps a byte[] array.
 * Used for in memory field values.
 */
@Immutable
public class DataBytes extends Data {
	public static final Data EMPTY = new DataBytes(new byte[0]);
	private final byte[] bytes;

	public DataBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public int length() {
		return bytes.length;
	}

	@Override
	public void addTo(ByteBuffer buf) {
		buf.put(bytes);
	}

	@Override
	public byte[] asArray() {
		return bytes;
	}

	@Override
	public byte byteAt(int i) {
		return bytes[i];
	}

}
