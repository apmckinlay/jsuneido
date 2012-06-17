/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

class MemStorage extends Storage {

	MemStorage() {
		super(32, 32);
	}

	MemStorage(int chunkSize, int initChunks) {
		super(align(chunkSize), initChunks);
	}

	@Override
	protected ByteBuffer get(int chunk) {
		return ByteBuffer.allocate(CHUNK_SIZE);
	}

}