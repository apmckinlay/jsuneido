/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

public interface Storage {
	int FIRST_ADR = 1;

	int alloc(int byteBufSize);

	/** Negative adr is relative to end */
	ByteBuffer buffer(int adr);

	/** @return Number of bytes from adr to current offset */
	long sizeFrom(int adr);

	void close();

	void protect();
	void protectAll();

	int advance(int adr, int length);

	boolean isValidPos(long pos);

	int checksum(int adr);

}
