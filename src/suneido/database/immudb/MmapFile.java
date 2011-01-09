/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Memory mapped file access.
 * <li>data is aligned to multiples of ALIGN (8)
 * <li>maximum allocation is CHUNK_SIZE (64mb)
 * <li>allocations cannot straddle chunks and will be bumped to next chunk
 * <li>offsets are divided by ALIGN and passed as int,
 * to reduce the space to store them
 * <li>therefore maximum file size is unsigned int max * ALIGN (32gb)
 * <p>
 * NOTE: When opening, trailing zero bytes are ignored.
 */
@ThreadSafe
public class MmapFile {
	private static final int SHIFT = 3;
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	private static final int ALIGN = (1 << SHIFT);
	private static final int MASK = ALIGN - 1;
	private static final int MB = 1024 * 1024;
	private static final int CHUNK_SIZE = 64 * MB;
	private static final int MAX_CHUNKS = (int) (MAX_SIZE / CHUNK_SIZE + 1);
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
	@GuardedBy("this")
	private long file_size;
	private final MappedByteBuffer[] fm = new MappedByteBuffer[MAX_CHUNKS];

	/** @param mode Must be "r" or "rw" */
	public MmapFile(String filename, String mode) {
		this(new File(filename), mode);
	}

	/** @param mode Must be "r" or "rw" */
	public MmapFile(File file, String mode) {
		if ("r".equals(mode)) {
			if (!file.canRead())
				throw new RuntimeException("can't open " + file + " read-only");
			this.mode = FileChannel.MapMode.READ_ONLY;
		} else if ("rw".equals(mode)) {
			if (file.exists() && (! file.canRead() || ! file.canWrite()))
				throw new RuntimeException("can't open " + file);
			this.mode = FileChannel.MapMode.READ_WRITE;
		} else
			throw new RuntimeException("invalid mode " + mode);
		try {
			fin = new RandomAccessFile(file, mode);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("can't open or create " + file, e);
		}
		fc = fin.getChannel();
		findEnd();
	}

	private void findEnd() {
		file_size = fileLength();
		if (file_size == 0)
			return;
		long offset = file_size;
		--offset;
		int chunk = (int) (offset / CHUNK_SIZE);
		map(chunk);
		ByteBuffer buf = fm[chunk];
		int i = (int) (offset % CHUNK_SIZE);
		while (i > 0 && buf.getLong(i - 8) == 0)
			i -= 8;
		file_size = (long) chunk * CHUNK_SIZE + align(i);
	}

	private long fileLength() {
		try {
			return fin.length();
		} catch (IOException e) {
			throw new RuntimeException("can't get file length", e);
		}
	}

	public synchronized int alloc(int n) {
		assert n < CHUNK_SIZE;
		n = align(n);

		// if insufficient room in this chunk, advance to next
		int remaining = CHUNK_SIZE - (int) (file_size % CHUNK_SIZE);
		if (n > remaining)
			file_size += remaining;

		long offset = file_size;
		file_size += n;
		return longToInt(offset);
	}

	private int align(int n) {
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	/** @returns A unique instance of a ByteBuffer
	 * extending from the offset to the end of the chunk.
	 */
	public ByteBuffer buffer(int adr) {
		long offset = intToLong(adr);
		ByteBuffer fmbuf = map(offset);
		synchronized(fmbuf) {
			fmbuf.position((int) (offset % CHUNK_SIZE));
			return fmbuf.slice();
		}
	}

	private synchronized ByteBuffer map(long offset) {
		assert 0 <= offset && offset < file_size;
		int chunk = (int) (offset / CHUNK_SIZE);
		if (fm[chunk] == null)
			try {
				fm[chunk] = fc.map(mode, (long) chunk * CHUNK_SIZE, CHUNK_SIZE);
				fm[chunk].order(ByteOrder.BIG_ENDIAN);
			} catch (IOException e) {
				throw new RuntimeException("MmapFile can't map chunk " + chunk, e);
			}
		return fm[chunk];
	}

	public void close() {
		Arrays.fill(fm, null); // might help gc
		try {
			fc.close();
			fin.close();
		} catch (IOException e) {
			throw new RuntimeException("MmapFile close failed", e);
		}
		// should truncate file but probably can't
		// since memory mappings won't all be finalized
		// so file size will be rounded up to chunk size
		// this is handled when re-opening
	}

	private static int longToInt(long n) {
		assert (n & MASK) == 0;
		assert n <= MAX_SIZE;
		return (int) (n >>> SHIFT);
	}

	private static long intToLong(int n) {
		return (n & 0xffffffffL) << SHIFT;
	}

}
