/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import suneido.SuException;
import suneido.util.Errlog;

/**
 * Memory mapped file access.
 * @see HeapStorage
 */
class MmapFile extends Storage {
	static final int MMAP_CHUNK_SIZE = 64 * 1024 * 1024; // 64 mb
	static final byte[] MAGIC = { 's', 'n', 'd', 'o' };
	static final ByteBuffer magic = ByteBuffer.allocate(4).put(MAGIC);
	static final int VERSION = 1;
	private final File file;
	private final FileChannel.MapMode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
	private final CopyOnWriteArrayList<MappedByteBuffer> toForce =
			new CopyOnWriteArrayList<>();
	private boolean open = false;
	private long lastForceSize;

	/** @param mode Must be "r" or "rw" */
	MmapFile(String filename, String mode) {
		this(new File(filename), mode);
	}

	/** @param mode Must be "r" or "rw" */
	MmapFile(File file, String mode) {
		super(MMAP_CHUNK_SIZE);
		this.file = file;
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
		storSize = fileLength();
		mapAll();
		findEnd();
		version();
		protect();
		lastForceSize = storSize;
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

	private long fileLength() {
		try {
			return Math.max(fin.length(), ALIGN);
		} catch (IOException e) {
			throw new SuException("can't get file length", e);
		}
	}

	void mapAll() {
		int n = offsetToChunk(storSize - 1);
		for (int i = 0; i <= n; ++i)
			chunks[i] = _get(i);
		toForce.add((MappedByteBuffer) chunks[n]);
	}

	/** handle zero padding caused by memory mapping */
	private void findEnd() {
		if (storSize <= ALIGN)
			return;
		if (0 != (storSize % ALIGN))
			return; // not aligned
		ByteBuffer buf = chunks[offsetToChunk(storSize - 1)];
		int i = (int) ((storSize - 1) % CHUNK_SIZE) + 1;
		assert ALIGN >= 8;
		while (i >= 8 && buf.getLong(i - 8) == 0)
			i -= 8;
		int chunk = (int) ((storSize - 1) / CHUNK_SIZE);
		storSize = (long) chunk * CHUNK_SIZE + align(i);
	}

	/**
	 * Does the actual memory mapping. Cached by Storage.
	 * @return the file mapping containing the specified offset
	 */
	@Override
	protected ByteBuffer get(int chunk) {
		if (! open)
			throw new RuntimeException("can't access database - it is not open");
		MappedByteBuffer bb = _get(chunk);
		toForce.add(bb);
		return bb;
	}

	private MappedByteBuffer _get(int chunk) {
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
	void force() {
		if (storSize == lastForceSize) // nothing written
			return;
		int n = 0;
		// snapshot iterator is only view of toForce
		// since it will be changing concurrently
		for (MappedByteBuffer bb : toForce)
			try {
				bb.force();
				++n;
				lastForceSize = storSize;
			} catch (Exception e) {
				// ignore intermittent IoExceptions on Windows
			}
		// all but the last can no longer change
		for (int i = n - 2; i >= 0; --i)
			toForce.remove(i);
		// this is needed to update file last modified time on Windows
		if (!file.setLastModified(System.currentTimeMillis()))
			Errlog.error("failed to setLastModified on " + file);
	}

	@Override
	protected void growChunks(int chunk) {
		Errlog.fatal("MmapFile chunks should not grow");
	}

	@Override
	public void close() {
		if (! open)
			return;
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
