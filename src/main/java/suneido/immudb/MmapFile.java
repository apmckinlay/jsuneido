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
	private static final int MMAP_CHUNK_SIZE = 64 * 1024 * 1024;
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
	private final long starting_file_size;
	private int last_force;
	private volatile boolean open = false;
	static final byte[] MAGIC = { 's', 'n', 'd', 'o' };
	static final int VERSION = 1;

	/** @param mode Must be "r" or "rw" */
	MmapFile(String filename, String mode) {
		this(new File(filename), mode);
	}

	/** @param mode Must be "r" or "rw" */
	MmapFile(File file, String mode) {
		super(MMAP_CHUNK_SIZE);
		switch (mode) {
		case "r":
			if (!file.canRead())
				throw new SuException("can't open " + file + " read-only");
			this.mode = FileChannel.MapMode.READ_ONLY;
			break;
		case "rw":
			if (file.exists() && (! file.canRead() || ! file.canWrite()))
				throw new SuException("can't open " + file + " read-write");
			this.mode = FileChannel.MapMode.READ_WRITE;
			break;
		default:
			throw new SuException("invalid mode " + mode);
		}
		try {
			fin = new RandomAccessFile(file, mode);
		} catch (FileNotFoundException e) {
			throw new SuException("can't open/create " + file, e);
		}
		fc = fin.getChannel();
		if (mode.equals("rw"))
			lock();
		open = true;
		findEnd();
		version();
		starting_file_size = storSize;
		last_force = offsetToChunk(storSize);
	}

	private void lock() {
		try {
			if (fc.tryLock() == null)
				throw new SuException("can't lock database file");
		} catch (IOException e) {
			throw new SuException("can't lock database file");
		}
	}

	private void version() {
		if (storSize == ALIGN) { // newly created file
			ByteBuffer buf = buf(0);
			buf.put(MAGIC).putInt(VERSION);
			assert buf.position() <= ALIGN;
		} else {
			ByteBuffer buf = buf(0);
			byte[] magic = new byte[4];
			buf.get(magic);
			if (!Arrays.equals(magic, MAGIC))
				throw new SuException("invalid database file");
			int ver = buf.getInt();
			if (ver != VERSION)
				throw new SuException("invalid database version, got " + ver +
						", expected " + VERSION);
		}
	}

	/** handle zero padding caused by memory mapping */
	private void findEnd() {
		storSize = Math.max(fileLength(), ALIGN);
		if (storSize <= ALIGN)
			return;
		if (0 != (storSize % ALIGN))
			return; // not aligned
		ByteBuffer buf = map(storSize - 1);
		int i = (int) ((storSize - 1) % CHUNK_SIZE) + 1;
		assert ALIGN >= 8;
		while (i >= 8 && buf.getLong(i - 8) == 0)
			i -= 8;
		int chunk = (int) ((storSize - 1) / CHUNK_SIZE);
		storSize = (long) chunk * CHUNK_SIZE + align(i);
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
		if (! open)
			throw new RuntimeException("can't access database - it is not open");
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
		if (storSize == starting_file_size) // nothing written
			return;

		for (int i = last_force; i <= offsetToChunk(storSize); ++i)
			if (chunks[i] != null)
				try {
					((MappedByteBuffer) chunks[i]).force();
					last_force = i;
				} catch (Exception e) {
					// ignore intermittent IoExceptions on Windows
				}

		// this is needed to update file last modified time on Windows
		try {
			fin.seek(storSize);
			fin.writeLong(0); // long since findEnd reads longs
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public void close() {
		open = false;
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
