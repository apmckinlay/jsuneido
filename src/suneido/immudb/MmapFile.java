/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

/**
 * Memory mapped file access.
 * <p>
 * When opening, trailing zero bytes are ignored.
 */
@ThreadSafe
class MmapFile extends ChunkedStorage {
	private static final int SHIFT = 3;
	private static final long MAX_SIZE = 0xffffffffL << SHIFT;
	private static final int MB = 1024 * 1024;
	private static final int CHUNK_SIZE = 64 * MB;
	private static final int MAX_CHUNKS = (int) (MAX_SIZE / CHUNK_SIZE + 1);
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
	private final File file;

	/** @param mode Must be "r" or "rw" */
	MmapFile(String filename, String mode) {
		this(new File(filename), mode);
	}

	/** @param mode Must be "r" or "rw" */
	MmapFile(File file, String mode) {
		super(CHUNK_SIZE, MAX_CHUNKS);
		this.file = file;
		if ("r".equals(mode)) {
			if (!file.canRead())
				throw new SuException("can't open " + file + " read-only");
			this.mode = FileChannel.MapMode.READ_ONLY;
		} else if ("rw".equals(mode)) {
			if (file.exists() && (! file.canRead() || ! file.canWrite()))
				throw new SuException("can't open " + file + " read-write");
			this.mode = FileChannel.MapMode.READ_WRITE;
		} else
			throw new SuException("invalid mode " + mode);
		try {
			fin = new RandomAccessFile(file, mode);
		} catch (FileNotFoundException e) {
			throw new SuException("can't open/create " + file, e);
		}
		fc = fin.getChannel();
		findEnd();
	}

	/** handle zero padding caused by memory mapping */
	private void findEnd() {
		file_size = fileLength();
		if (file_size == 0)
			return;
		assert file_size % ALIGN == 0;
		ByteBuffer buf = map(file_size - 1);
		int i = (int) ((file_size - 1) % CHUNK_SIZE) + 1;
		assert ALIGN >= 8;
		while (i > 0 && buf.getLong(i - 8) == 0)
			i -= 8;
		int chunk = (int) ((file_size - 1) / CHUNK_SIZE);
		file_size = (long) chunk * CHUNK_SIZE + align(i);
	}

	private long fileLength() {
		try {
			return fin.length();
		} catch (IOException e) {
			throw new SuException("can't get file length", e);
		}
	}

	/** @return the file mapping containing the specified offset */
	@Override
	protected synchronized ByteBuffer get(int chunk) {
		long pos = (long) chunk * CHUNK_SIZE;
		long len = mode == FileChannel.MapMode.READ_WRITE
				? CHUNK_SIZE : Math.min(CHUNK_SIZE, file_size - pos);
		try {
			return fc.map(mode, pos, len);
		} catch (IOException e) {
			throw new SuException("MmapFile can't map chunk " + chunk, e);
		}
	}

	@Override
	public void close() {
if (mode == FileChannel.MapMode.READ_WRITE)
System.out.println(file + " size " + file_size);
		try {
			fc.close();
			fin.close();
		} catch (IOException e) {
			throw new SuException("MmapFile close failed", e);
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
