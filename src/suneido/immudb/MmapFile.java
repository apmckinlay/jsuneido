/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

/**
 * Memory mapped file access.
 * <p>
 * When opening, trailing zero bytes are ignored.
 * @see HeapStorage
 */
@ThreadSafe
class MmapFile extends Storage {
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
	final File file;
	long starting_file_size;
	private int last_force;

	/** @param mode Must be "r" or "rw" */
	MmapFile(String filename, String mode) {
		this(new File(filename), mode);
	}

	/** @param mode Must be "r" or "rw" */
	MmapFile(File file, String mode) {
		super(64 * 1024 * 1024);
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
		last_force = offsetToChunk(storSize);
	}

	/** handle zero padding caused by memory mapping */
	private void findEnd() {
		storSize = fileLength();
		if (storSize == 0)
			return;
		assert storSize % ALIGN == 0;
		ByteBuffer buf = map(storSize - 1);
		int i = (int) ((storSize - 1) % CHUNK_SIZE) + 1;
		assert ALIGN >= 8;
		while (i > 0 && buf.getLong(i - 8) == 0)
			i -= 8;
		int chunk = (int) ((storSize - 1) / CHUNK_SIZE);
		storSize = (long) chunk * CHUNK_SIZE + align(i);
		starting_file_size = storSize;
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
				? CHUNK_SIZE : Math.min(CHUNK_SIZE, storSize - pos);
		try {
			return fc.map(mode, pos, len);
		} catch (IOException e) {
			throw new SuException("MmapFile can't map chunk " + chunk, e);
		}
	}

	@Override
	synchronized void force() {
		for (int i = last_force; i <= offsetToChunk(storSize); ++i)
			if (chunks[i] != null)
				try {
					((MappedByteBuffer) chunks[i]).force();
					last_force = i;
				} catch (Exception e) {
					// ignore intermittent IoExceptions on Windows
				}
	}

	@Override
	void close() {
//		if (mode == FileChannel.MapMode.READ_WRITE)
//			System.out.println(file + " size " + file_size +
//					" grew " + (file_size - starting_file_size));
		force();
		Arrays.fill(chunks, null); // might help gc
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

}
