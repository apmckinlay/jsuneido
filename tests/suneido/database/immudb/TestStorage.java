/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

class TestStorage extends ChunkedStorage {
	TestStorage() {
		super(32, 16);
	}

	TestStorage(int chunkSize, int maxChunks) {
		super(chunkSize, maxChunks);
	}

	@Override
	protected ByteBuffer get(int chunk) {
		return ByteBuffer.allocate(CHUNK_SIZE);
	}

}