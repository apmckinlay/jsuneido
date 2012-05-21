/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

public interface Storage {
	int FIRST_ADR = 1;

	int alloc(int byteBufSize);

	/**
	 * @param adr Negative value is relative to end.
	 * @returns A unique instance of a ByteBuffer
	 * i.e. not shared so it may be modified.
	 * extending from the offset to the end of the chunk.
	 */
	ByteBuffer buffer(int adr);

	/**
	 * Faster than buffer because it does not duplicate and slice.
	 * @return The buffer containing the address.
	 * NOTE: This ByteBuffer is shared and must not be modified.
	 */
	ByteBuffer bufferBase(int adr);

	/** @return The position of adr in bufferBase */
	int bufferPos(int adr);

	/** @return Number of bytes from adr to current offset */
	long sizeFrom(int adr);

	void close();

	void protect();
	void protectAll();

	int advance(int adr, int length);

	boolean isValidPos(long pos);

	int checksum(int adr);

}
