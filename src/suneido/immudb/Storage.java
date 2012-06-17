/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.primitives.Longs;

/**
 * Chunked storage access.
 * <li>data is aligned to multiples of ALIGN (8)
 * <li>maximum allocation is CHUNK_SIZE
 * <li>allocations cannot straddle chunks and will be bumped to next chunk
 * <li>long offsets are divided by ALIGN and passed as int,
 * to reduce the space to store them
 * <li>therefore maximum file size is unsigned int max * ALIGN (32gb)
 * <li>blocks should not start with (long) 0 since that is used to detect padding
 */
@ThreadSafe
abstract class Storage {
	static final int FIRST_ADR = 1;
	private static final int SHIFT = 3;
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	static final int ALIGN = (1 << SHIFT); // must be power of 2
	private static final int MASK = ALIGN - 1;
	protected ByteBuffer[] chunks;
	final int CHUNK_SIZE;
	protected volatile long file_size;

	Storage(int chunkSize, int initChunks) {
		CHUNK_SIZE = chunkSize;
		chunks = new ByteBuffer[initChunks];
	}

	/**
	 * Allocate a block of storage.
	 * It will be aligned, and may require advancing to next chunk.
	 * (Leaving padding filled with zero bytes.)
	 * @param n The size of the block required.
	 * @return The "address" of the block. (Not just an offset.)
	 */
	synchronized int alloc(int n) {
		assert n < CHUNK_SIZE : n + " not < " + CHUNK_SIZE;
		n = align(n);

		// if insufficient room in this chunk, advance to next
		int remaining = CHUNK_SIZE - (int) (file_size % CHUNK_SIZE);
		if (n > remaining)
			file_size += remaining;

		long offset = file_size;
		file_size += n;

		int chunk = offsetToChunk(offset);
		if (chunk >= chunks.length)
			growChunks(chunk);

		return offsetToAdr(offset);
	}

	private void growChunks(int chunk) {
		chunks = Arrays.copyOf(chunks, 2 * chunk);
	}

	static int align(int n) {
		// requires ALIGN to be power of 2
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	int advance(int adr, int length) {
		long offset = posToOffset(adr);
		offset += align(length);
		if (offset < file_size) {
			ByteBuffer buf = buf(offset);
			if (buf.getLong() == 0)
				// skip end of chunk padding
				offset += Longs.BYTES + buf.remaining();
		}
		return offsetToAdr(offset);
	}

	/**
	 * @param adr Negative value is relative to end.
	 * @returns A unique instance of a ByteBuffer
	 * i.e. not shared so it may be modified.
	 * extending from the offset to the end of the chunk.
	 */
	ByteBuffer buffer(int adr) {
		assert adr != 0 : "storage address should never be 0";
		return buf(posToOffset(adr));
	}

	/**
	 * Faster than buffer because it does not duplicate and slice.
	 * @return The buffer containing the address.
	 * NOTE: This ByteBuffer is shared and must not be modified.
	 */
	ByteBuffer bufferBase(int adr) {
		return map(posToOffset(adr));
	}

	/** @return The position of adr in bufferBase */
	int bufferPos(int adr) {
		return (int) (posToOffset(adr) % CHUNK_SIZE);
	}

	/**
	 * @param pos Is either an address
	 * or a negative offset from the end of the file
	 */
	private long posToOffset(int pos) {
		return pos < 0 ? file_size + pos : adrToOffset(pos);
	}

	private long protect = 0;

	void protect() {
		assert protect == 0 || protect == Integer.MAX_VALUE;
		protect = file_size;
	}

	void protectAll() {
		assert protect != Integer.MAX_VALUE;
		protect = Integer.MAX_VALUE;
	}

	private ByteBuffer buf(long offset) {
		ByteBuffer buf = map(offset);
		buf = (offset < protect) ? buf.asReadOnlyBuffer() : buf.duplicate();
		buf.position((int) (offset % CHUNK_SIZE));
		long startOfLastChunk = (file_size / CHUNK_SIZE) * CHUNK_SIZE;
		if (offset >= startOfLastChunk)
			buf.limit((int) (file_size - startOfLastChunk));
		return buf.slice();
	}

	/** @return the chunk containing the specified offset */
	protected ByteBuffer map(long offset) {
		assert 0 <= offset && offset < file_size;
		int chunk = offsetToChunk(offset);
		if (chunks[chunk] == null) {
			chunks[chunk] = get(chunk);
			chunks[chunk].order(ByteOrder.BIG_ENDIAN);
		}
		return chunks[chunk];
	}

	protected int offsetToChunk(long offset) {
		return (int) (offset / CHUNK_SIZE);
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

	/** @return checksum for bytes from adr to end of file */
	int checksum(int adr) {
		Checksum cksum = new Checksum();
		long offset = posToOffset(adr);
		while (offset < file_size) {
			ByteBuffer buf = buf(offset);
			offset += buf.remaining();
			cksum.update(buf);
		}
		return cksum.getValue();
	}

	/** @return Number of bytes from adr to current offset */
	long sizeFrom(int adr) {
		return adr == 0 ? file_size : file_size - posToOffset(adr);
	}

	boolean isValidPos(long pos) {
		if (pos < 0)
			pos += file_size;
		return 0 <= pos && pos < file_size;
	}

	void force() {
	}

	void close() {
	}

}
