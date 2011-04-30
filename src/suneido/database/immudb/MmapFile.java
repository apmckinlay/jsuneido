/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Memory mapped file access.
 * <p>
 * When opening, trailing zero bytes are ignored.
 */
@ThreadSafe
public class MmapFile extends ChunkedStorage {
	private static final int SHIFT = 3;
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	private static final int MB = 1024 * 1024;
	private static final int CHUNK_SIZE = 64 * MB;
	private static final int MAX_CHUNKS = (int) (MAX_SIZE / CHUNK_SIZE + 1);
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;

	/** @param mode Must be "r" or "rw" */
	public MmapFile(String filename, String mode) {
		this(new File(filename), mode);
	}

	/** @param mode Must be "r" or "rw" */
	public MmapFile(File file, String mode) {
		super(CHUNK_SIZE, MAX_CHUNKS);
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

	/** handle zero padding caused by memory mapping */
	private void findEnd() {
		file_size = fileLength();
		if (file_size == 0)
			return;
		ByteBuffer buf = map(file_size - 1);
		int i = (int) ((file_size - 1) % CHUNK_SIZE) + 1;
		while (i > 0 && buf.getLong(i - 8) == 0)
			i -= 8;
		int chunk = (int) ((file_size - 1) / CHUNK_SIZE);
		file_size = (long) chunk * CHUNK_SIZE + align(i);
	}

	private long fileLength() {
		try {
			return fin.length();
		} catch (IOException e) {
			throw new RuntimeException("can't get file length", e);
		}
	}

	/** @return the file mapping containing the specified offset */
	@Override
	protected synchronized ByteBuffer get(int chunk) {
		try {
			return fc.map(mode, (long) chunk * CHUNK_SIZE, CHUNK_SIZE);
		} catch (IOException e) {
			throw new RuntimeException("MmapFile can't map chunk " + chunk, e);
		}
	}

	public void close() {
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

	public static void main(String[] args) {
		MmapFile mmf = new MmapFile("immu.db", "r");
		System.out.println(mmf.file_size);
	}

}
