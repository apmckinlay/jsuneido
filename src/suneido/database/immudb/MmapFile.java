/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Memory mapped file access.
 * <li>data is aligned to multiples of ALIGN (8)
 * <li>maximum allocation is CHUNK_SIZE (64mb)
 * <li>allocations cannot straddle chunks and will be bumped to next chunk
 * <li>maximum file size is MAX_CHUNKS * CHUNK_SIZE (32gb)
 * <p>
 * NOTE: When opening, trailing zero bytes are ignored.
 */
@NotThreadSafe
public class MmapFile {
	public static final int ALIGN = 8;
	private static final int MB = 1024 * 1024;
	public static final int CHUNK_SIZE = 64 * MB;
	public static final int MAX_CHUNKS = 512;
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
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
		file_size = findEnd();
	}

	private long findEnd() {
		long offset = fileLength();
		if (offset == 0)
			return 0;
		--offset;
		int chunk = (int) (offset / CHUNK_SIZE);
		map(chunk);
		ByteBuffer buf = fm[chunk];
		int i = (int) (offset % CHUNK_SIZE);
		while (i > 0 && buf.get(i - 1) == 0)
			--i;
		return (long) chunk * CHUNK_SIZE + align(i);
	}

	private long fileLength() {
		try {
			return fin.length();
		} catch (IOException e) {
			throw new RuntimeException("can't get file length", e);
		}
	}

	public long alloc(int n) {
		assert n < CHUNK_SIZE;
		n = align(n);

		// if insufficient room in this chunk, advance to next
		int remaining = CHUNK_SIZE - (int) (file_size % CHUNK_SIZE);
		if (n > remaining)
			file_size += remaining;

		long offset = file_size;
		file_size += n;
		return offset;
	}

	private int align(int n) {
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	/** @returns A ByteBuf extending from the offset to the end of the chunk */
//	public ByteBuf buf(long offset) {
//		assert offset >= 0;
//		assert offset < file_size
//				: "offset " + offset + " should be < file size " + file_size;
//		int chunk = (int) (offset / CHUNK_SIZE);
//		map(chunk);
//		synchronized(fm[chunk]) {
//			return ByteBuf.wrap(fm[chunk], (int) (offset % CHUNK_SIZE));
//		}
//	}

	/** @returns A unique instanced of a ByteBuffer
	 * extending from the offset to the end of the chunk.
	 */
	public ByteBuffer buffer(long offset) {
		assert offset >= 0;
		assert offset < file_size;
		int chunk = (int) (offset / CHUNK_SIZE);
		map(chunk);
		ByteBuffer buf = fm[chunk];
		synchronized(buf) {
			try {
				buf.position((int) (offset % CHUNK_SIZE));
				return buf.slice();
			} finally {
				buf.position(0);
			}
		}
	}

	private synchronized void map(int chunk) {
		if (fm[chunk] != null)
			return;
		try {
			fm[chunk] = fc.map(mode, (long) chunk * CHUNK_SIZE, CHUNK_SIZE);
			fm[chunk].order(ByteOrder.LITTLE_ENDIAN);
		} catch (IOException e) {
			throw new RuntimeException("MmapFile can't map chunk " + chunk, e);
		}
	}

	public long size() {
		return file_size;
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
}
