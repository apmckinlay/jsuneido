/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.common.primitives.UnsignedInts;

// TODO eliminate +1 on addresses
// since reserving the first unit of storage prevents a zero address
// BUT this will change the database i.e. require a version increment

/**
 * Chunked storage access. Abstract base class for MemStorage and MmapFile.
 * <ul>
 * <li>derived classes must set storSize
 * <li>data is aligned to multiples of ALIGN (8)
 * <li>maximum allocation is CHUNK_SIZE
 * <li>allocations can't straddle chunks and will be bumped to next chunk
 * <li>long offsets are divided by ALIGN and passed as int "addresses" (adr),
 * 		to reduce the space to store them.
 *		Addresses are really unsigned ints, but we use int since that's all Java has.
 *		WARNING: must use Integer.compareUnsigned, NOT < or >
 * 		To keep 0 as a special value, addresses start at 1.
 * 		See offsetToAdr and adrToOffset.
 * <li>therefore maximum file size is unsigned int max * ALIGN (32gb)
 * <li>blocks should not start with (long) 0 since that is used to detect padding
 * </ul>
 * WARNING: Operations are <b>not</b> synchronized.
 * In particular, access to the chunks array is <b>not</b> thread safe.
 * Thread safety and visibility must be ensured externally.
 * <ul>
 * <li>For visibility, readers must acquire a lock when starting
 * 		and writers must release the <b>same</b> lock when ending
 * <li>Writers must hold the commit lock (only one writer at a time)
 * <li>Update transactions call trans.commit/abort at end,
 * 		read transactions call trans.add at beginning
 * <li>Database.check gets commit lock at start,
 * 		writers release commit lock at end
 * </ul>
 */
abstract class Storage implements AutoCloseable {
	protected final static int FIRST_ADR = 2;
	protected final static int MAX_ADR = 0xffffffff;
	protected static final int SHIFT = 3; // i.e. 8 byte alignment
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	static final int ALIGN = (1 << SHIFT); // must be power of 2
	protected static final int MASK = ALIGN - 1;
	final int CHUNK_SIZE;
	/** INIT_CHUNKS should be the max for database chunk size & align
	 * i.e. unsigned int max * align / chunk size
	 * so that chunks never grow, to avoid concurrency issues.
	 * Ok to grow for temp index storage since it's not concurrent */
	protected final int INIT_CHUNKS = 512;
	protected ByteBuffer[] chunks = new ByteBuffer[INIT_CHUNKS];
	protected long storSize = ALIGN; // one unit reserved
	private long protect = 0;

	Storage(int chunkSize) {
		CHUNK_SIZE = align(chunkSize);
	}

	/**
	 * Allocate a block of storage.
	 * It will be aligned, and may require advancing to next chunk.
	 * (Leaving padding filled with zero bytes.)
	 * This is where chunks are mapped by calling get.
	 * @param n The size of the block required.
	 * @return The "address" of the block. (Not just an offset.)
	 */
	int alloc(int n) {
		assert 0 <= n && n < CHUNK_SIZE;
		n = align(n);

		// if insufficient room in this chunk, advance to next
		int remaining = CHUNK_SIZE - (int) (storSize % CHUNK_SIZE);
		if (n > remaining)
			storSize += remaining;
		int chunk = offsetToChunk(storSize);
		if (chunk >= chunks.length)
			growChunks(chunk);
		if (chunks[chunk] == null)
			chunks[chunk] = get(chunk); // map
		long offset = storSize;
		storSize += n;
		return offsetToAdr(offset);
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
			int remaining = buf.remaining();
			if (allZero(buf))
				offset += remaining; // skip trailing chunk padding
		}
		return offsetToAdr(offset);
	}

	private static boolean allZero(ByteBuffer buf) {
		while (buf.remaining() > 0)
			if (buf.get() != 0)
				return false;
		return true;
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
		assert -rpos <= storSize;
		return offsetToAdr(storSize + rpos);
	}

	/**
	 * Faster than buffer because it does not duplicate and slice.<p>
	 * NOTE: This ByteBuffer is shared and must not be modified.
	 * @return The buffer containing the address.
	 */
	ByteBuffer bufferBase(int adr) {
		return chunks[offsetToChunk(adrToOffset(adr))];
	}

	/** @return The position of adr in bufferBase */
	int bufferPos(int adr) {
		return (int) (adrToOffset(adr) % CHUNK_SIZE);
	}

	void protect() {
		protect = storSize;
	}

	protected ByteBuffer buf(long offset) {
		ByteBuffer buf = chunks[offsetToChunk(offset)];
		if (offset < protect)
			buf = buf.asReadOnlyBuffer();
		int pos = (int) (offset % CHUNK_SIZE);
		long startOfLastChunk = (storSize / CHUNK_SIZE) * CHUNK_SIZE;
		int limit = (offset >= startOfLastChunk)
				? (int) (storSize - startOfLastChunk)
				: buf.limit();
		return buf.slice(pos, limit - pos);
	}

	protected int offsetToChunk(long offset) {
		return (int) (offset / CHUNK_SIZE);
	}

	protected void growChunks(int chunk) {
		chunks = Arrays.copyOf(chunks, (3 * chunk) / 2);
	}

	protected abstract ByteBuffer get(int chunk);

	static int offsetToAdr(long n) {
		assert (n & MASK) == 0;
		assert n <= MAX_SIZE;
		return (int) (n >>> SHIFT) + 1; // +1 to avoid 0
	}

	static long adrToOffset(int adr) {
		assert adr != 0;
		return UnsignedInts.toLong(adr - 1) << SHIFT;
	}

	/**
	 * Convert a long size up to unsigned int max.
	 * Throw if out of range.
	 * NOTE: this approach only handles sizes up to 4gb.
	 * This is a problem if a table or index > 4gb
	 * because load puts entire table / index into one commit.
	 */
	static int sizeToInt(long size) {
		assert (size & MASK) == 0;
		size = size >>> SHIFT;
		assert size < 0x100000000L; // unsigned int max
		return (int) size;
	}

	/** convert an unsigned int to a long size */
	static long intToSize(int size) {
		return UnsignedInts.toLong(size) << SHIFT;
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

	/**
	 * @return Number of bytes from adr to current offset.
	 * treats 0 as start of file (which is actually 1)
	 */
	long sizeFrom(int adr) {
		long size = storSize; // read once to avoid concurrency issues
		return adr == 0 ? size : size - adrToOffset(adr);
	}

	/** @return a limit address i.e. just past the end of the current data */
	int upTo() {
		return offsetToAdr(storSize);
	}

	boolean isValidPos(long pos) {
		long size = storSize; // read once to avoid concurrency issues
		if (pos < 0)
			pos += size;
		return ALIGN <= pos && pos < size;
	}

	boolean isValidAdr(int adr) {
		long off = adrToOffset(adr);
		return 0 <= off && off < storSize;
	}

	// used by check/rebuild
	void setSize(long size) {
		storSize = size;
	}

	void force() {
	}

	@Override
	public void close() {
	}

}
