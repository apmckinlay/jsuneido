/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

@Immutable
public class DataBytes extends Data {
	private final byte[] bytes;

	public DataBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public int size() {
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

}
