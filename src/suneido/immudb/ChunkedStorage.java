/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkElementIndex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.collect.AbstractIterator;

/**
 * Chunked storage access.
 * <li>data is aligned to multiples of ALIGN (8)
 * <li>maximum allocation is CHUNK_SIZE
 * <li>allocations cannot straddle chunks and will be bumped to next chunk
 * <li>long offsets are divided by ALIGN and passed as int,
 * to reduce the space to store them
 * <li>therefore maximum file size is unsigned int max * ALIGN (32gb)
 */
@ThreadSafe
abstract class ChunkedStorage implements Storage {
	private static final int SHIFT = 3;
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	static final int ALIGN = (1 << SHIFT); // must be power of 2
	private static final int MASK = ALIGN - 1;
	protected final ByteBuffer[] chunks;
	protected final int CHUNK_SIZE;
	protected long file_size;

	ChunkedStorage(int chunkSize, int maxChunks) {
		CHUNK_SIZE = chunkSize;
		chunks = new ByteBuffer[maxChunks];
	}

	@Override
	public synchronized int alloc(int n) {
		assert n < CHUNK_SIZE : n + " not < " + CHUNK_SIZE;
		n = align(n);

		// if insufficient room in this chunk, advance to next
		int remaining = CHUNK_SIZE - (int) (file_size % CHUNK_SIZE);
		if (n > remaining)
			file_size += remaining;

		long offset = file_size;
		file_size += n;
		return offsetToAdr(offset);
	}

	static int align(int n) {
		// requires ALIGN to be power of 2
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	@Override
	public int advance(int adr, int length) {
		long offset = posToOffset(adr);
		offset += align(length);
		return offsetToAdr(offset);
	}

	/**
	 * @returns A unique instance of a ByteBuffer
	 * extending from the offset to the end of the chunk.
	 */
	@Override
	public ByteBuffer buffer(int adr) {
		return buf(posToOffset(adr));
	}

	/**
	 * @param pos Is either an address
	 * or a negative offset from the end of the file
	 */
	private long posToOffset(int pos) {
		return pos < 0 ? file_size + pos : adrToOffset(pos);
	}

	private long protect = 0;

	@Override
	public void protect() {
		assert protect == 0 || protect == Integer.MAX_VALUE;
		protect = file_size;
	}

	@Override
	public void protectAll() {
		assert protect != Integer.MAX_VALUE;
		protect = Integer.MAX_VALUE;
	}

	private synchronized ByteBuffer buf(long offset) {
		ByteBuffer buf = map(offset);
		long startOfLastChunk = (file_size / CHUNK_SIZE) * CHUNK_SIZE;
		buf.limit((offset < startOfLastChunk)
			? CHUNK_SIZE
			: (int) (file_size - startOfLastChunk));
		buf.position((int) (offset % CHUNK_SIZE));
// TODO remove in production (10% faster for load)
if (offset < protect)
return buf.slice().asReadOnlyBuffer();
		return buf.slice();
	}

	/** @return the chunk containing the specified offset */
	protected ByteBuffer map(long offset) {
		checkElementIndex((int) offset, (int) file_size);
		int chunk = (int) (offset / CHUNK_SIZE);
		if (chunks[chunk] == null) {
			chunks[chunk] = get(chunk);
			chunks[chunk].order(ByteOrder.BIG_ENDIAN);
		}
		return chunks[chunk];
	}

	protected abstract ByteBuffer get(int chunk);

	static int offsetToAdr(long n) {
		assert (n & MASK) == 0;
		assert n <= MAX_SIZE;
		return (int) (n >>> SHIFT) + 1; // +1 to avoid 0
	}

	static long adrToOffset(int n) {
		return ((n - 1) & 0xffffffffL) << SHIFT;
	}

	@Override
	public Iterator<ByteBuffer> iterator(int adr) {
		return new Iter(adr);
	}

	private class Iter extends AbstractIterator<ByteBuffer> {
		private long offset;

		Iter(int adr) {
			offset = posToOffset(adr);
		}

		@Override
		protected ByteBuffer computeNext() {
			if (offset < file_size) {
				ByteBuffer buf = buf(offset);
				offset += buf.remaining();
				return buf;
			}
			return endOfData();
		}

	}

	@Override
	public long sizeFrom(int adr) {
		return adr == 0 ? file_size : file_size - posToOffset(adr);
	}

	@Override
	public boolean isValidPos(long pos) {
		if (pos < 0)
			pos += file_size;
		return 0 <= pos && pos < file_size;
	}

}
