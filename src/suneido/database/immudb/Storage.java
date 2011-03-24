/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;

public interface Storage {
	static final int FIRST_ADR = 1;

	int alloc(int byteBufSize);

	/** negative adr is relative to end */
	ByteBuffer buffer(int adr);

	/** iterates through raw storage, NOT the allocation blocks */
	Iterator<ByteBuffer> iterator(int adr);

	/** number of bytes from adr to current offset */
	int sizeFrom(int adr);

}
