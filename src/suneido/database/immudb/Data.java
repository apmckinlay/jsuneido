/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * Abstract wrapper for either a byte[] array or a slice of a ByteBuffer
 */
@Immutable
public abstract class Data {
	public static final Data EMPTY = new Empty();

	public abstract int size();

	public abstract void addTo(ByteBuffer buf);

	public abstract byte[] asArray();

	private static class Empty extends Data {
		private static final byte[] empty = new byte[0];
		@Override
		public int size() {
			return 0;
		}
		@Override
		public void addTo(ByteBuffer buf) {
		}
		@Override
		public byte[] asArray() {
			return empty;
		}
	}

}
