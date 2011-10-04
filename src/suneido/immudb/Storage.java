/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Storage {
	int FIRST_ADR = 1;

	int alloc(int byteBufSize);

	/** Negative adr is relative to end */
	ByteBuffer buffer(int adr);

	/**
	 * Iterates through raw storage, NOT the allocation blocks.
	 * Negative adr is relative to end.
	 */
	Iterator<ByteBuffer> iterator(int adr);

	/** @return Number of bytes from adr to current offset */
	long sizeFrom(int adr);

	void close();

	void protect();
	void protectAll();

	int advance(int adr, int length);

}
