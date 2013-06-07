/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

/**
 * In-memory storage used by {@link RecordStore} and tests.
 * @see MmapFile
 */
class HeapStorage extends Storage {
	private final static int DEFAULT_CHUNK_SIZE = 1024;

	HeapStorage() {
		super(DEFAULT_CHUNK_SIZE);
	}

	HeapStorage(int chunkSize) {
		super(chunkSize);
	}

	@Override
	protected ByteBuffer get(int chunk) {
		return ByteBuffer.allocate(CHUNK_SIZE);
	}

}