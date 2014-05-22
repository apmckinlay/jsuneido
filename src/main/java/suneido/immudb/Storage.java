/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedInts;

/**
 * Chunked storage access. Abstract base class for MemStorage and MmapFile.
 * <li>derived classes must set storSize
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
	protected static final int SHIFT = 3;
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	static final int ALIGN = (1 << SHIFT); // must be power of 2
	private static final int MASK = ALIGN - 1;
	protected ByteBuffer[] chunks = new ByteBuffer[32];
	protected final int CHUNK_SIZE;
	protected volatile long storSize = 0;

	Storage(int chunkSize) {
		CHUNK_SIZE = align(chunkSize);
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
		int remaining = CHUNK_SIZE - (int) (storSize % CHUNK_SIZE);
		if (n > remaining)
			storSize += remaining;

		long offset = storSize;
		storSize += n;

		int chunk = offsetToChunk(offset);
		if (chunk >= chunks.length)
			growChunks(chunk);

		return offsetToAdr(offset);
	}

	private void growChunks(int chunk) {
		chunks = Arrays.copyOf(chunks, (3 * chunk) / 2);
	}

	static int align(int n) {
		// requires ALIGN to be power of 2
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	static long align(long n) {
		// requires ALIGN to be power of 2
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	int advance(int adr, long length) {
		long offset = adrToOffset(adr);
		offset += align(length);
		if (offset < storSize) {
			ByteBuffer buf = buf(offset);
			if (buf.getLong() == 0)
				// skip end of chunk padding
				offset += Longs.BYTES + buf.remaining();
		}
		return offsetToAdr(offset);
	}

	/**
	 * @param offset An address (not an offset) as returned by alloc
	 * @returns A unique instance of a ByteBuffer
	 * i.e. not shared so it may be modified.
	 * extending from the offset to the end of the chunk.
	 */
	ByteBuffer buffer(int adr) {
		assert adr != 0 : "storage address should never be 0";
		return buf(adrToOffset(adr));
	}

	/**
	 * @param rpos A negative offset from the end of the file
	 */
	ByteBuffer rbuffer(long rpos) {
		assert rpos < 0;
		return buf(storSize + rpos);
	}

	int rposToAdr(long rpos) {
		assert rpos < 0;
		return offsetToAdr(storSize + rpos);
	}

	/**
	 * Faster than buffer because it does not duplicate and slice.<p>
	 * NOTE: This ByteBuffer is shared and must not be modified.
	 * @return The buffer containing the address.
	 */
	ByteBuffer bufferBase(int adr) {
		return map(adrToOffset(adr));
	}

	/** @return The position of adr in bufferBase */
	int bufferPos(int adr) {
		return (int) (adrToOffset(adr) % CHUNK_SIZE);
	}

	private long protect = 0;

	void protect() {
		assert protect == 0 || protect == Integer.MAX_VALUE;
		protect = storSize;
	}

	void protectAll() {
		assert protect != Integer.MAX_VALUE;
		protect = Integer.MAX_VALUE;
	}

	private ByteBuffer buf(long offset) {
		ByteBuffer buf = map(offset);
		buf = (offset < protect) ? buf.asReadOnlyBuffer() : buf.duplicate();
		buf.position((int) (offset % CHUNK_SIZE));
		long startOfLastChunk = (storSize / CHUNK_SIZE) * CHUNK_SIZE;
		if (offset >= startOfLastChunk)
			buf.limit((int) (storSize - startOfLastChunk));
		return buf.slice();
	}

	/** @return the chunk containing the specified offset */
	protected ByteBuffer map(long offset) {
		assert 0 <= offset && offset < storSize;
		int chunk = offsetToChunk(offset);
		if (chunk >= chunks.length)
			growChunks(chunk);
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

	static long adrToOffset(int adr) {
		return ((adr - 1) & 0xffffffffL) << SHIFT;
	}

	/**
	 * Convert a long size up to unsigned int max.
	 * Throw if out of range.
	 * NOTE: this approach only handles sizes up to 4gb.
	 * This is a problem if a table or index > 4gb
	 * because load puts entire table / index into one commit.
	 */
	static int sizeToInt(long size) {
		assert size < 0x100000000L; // unsigned int max
		return (int) size;
	}

	/** convert an unsigned int to a long size */
	static long intToSize(int size) {
		return UnsignedInts.toLong(size);
	}

	/** @return checksum for bytes from adr to end of file */
	int checksum(int adr) {
		Checksum cksum = new Checksum();
		long offset = adrToOffset(adr);
		while (offset < storSize) {
			ByteBuffer buf = buf(offset);
			offset += buf.remaining();
			cksum.update(buf);
		}
		return cksum.getValue();
	}

	/** @return Number of bytes from adr to current offset */
	long sizeFrom(int adr) {
		return adr == 0 ? storSize : storSize - adrToOffset(adr);
	}

	boolean isValidPos(long pos) {
		if (pos < 0)
			pos += storSize;
		return 0 <= pos && pos < storSize;
	}

	boolean isValidAdr(int adr) {
		long off = adrToOffset(adr);
		return 0 <= off && off < storSize;
	}

	void force() {
	}

	void close() {
	}

}
