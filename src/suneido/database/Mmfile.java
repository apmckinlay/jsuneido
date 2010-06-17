package suneido.database;

import static suneido.SuException.fatal;
import static suneido.SuException.verify;
import static suneido.Suneido.errlog;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Iterator;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.util.ByteBuf;

/**
 * Memory mapped file access using Java NIO. Maps in 4mb chunks. A maximum of
 * 1gb is mapped at any one time. Chunks are unmapped as necessary roughly LRU
 * using a clock method. Individual blocks must fit in a single chunk. Blocks
 * are aligned on 8 byte boundaries allowing offsets to be shifted right to fit
 * in 32 bit int's. Alignment also allows storing a type in the low bits of the
 * block size. Since offsets are stored shifted as int's maximum file size is
 * 32gb (max unsigned int (4gb) << 3).
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
@ThreadSafe
public class Mmfile extends Destination implements Iterable<ByteBuf> {
	private final Mode mode;
	private final RandomAccessFile fin;
	private final FileChannel fc;
	private final MappedByteBuffer[] fm = new MappedByteBuffer[MAX_CHUNKS];
	private final long[] last_used = new long[MAX_CHUNKS];
	private volatile long file_size;
	private int chunks_mapped = 0;
	private int hi_chunk = 0;
	private long clock = 0;

	private final static int HEADER = 4; // contains size | type
	private final static int TRAILER = 4; // contains size ^ adr
	final static int OVERHEAD = HEADER + TRAILER;
	final static int ALIGN = 8;
	final static int SHIFT = 2;
	private final static int MB_PER_CHUNK = 4;
	private final static int MAX_CHUNKS_MAPPED = 1024 / MB_PER_CHUNK;
	private final static int MB_MAX_DB = 32 * 1024; // 32 gb
	private final static int MAX_CHUNKS = MB_MAX_DB / MB_PER_CHUNK;
	private final static int FILEHDR = 8; // should be multiple of align
	private final static byte[] magic = new byte[] { 'S', 'n', 'd', 'o' };
	private final static int FILESIZE_OFFSET = 4;
	private final static int BEGIN_OFFSET = FILEHDR + HEADER;
	private final static byte FILLER = 0;
	public final static byte DATA = 1;
	public final static byte COMMIT = 2;
	public final static byte SESSION = 3;
	public final static byte OTHER = 4;
	private static enum MmCheck {
		OK, ERR, EOF
	};

	// these are only overridden for tests
	private int chunk_size = MB_PER_CHUNK * 1024 * 1024;
	private int max_chunks_mapped = MAX_CHUNKS_MAPPED;


	public Mmfile(String filename, Mode mode) {
		this(new File(filename), mode);
	}

	public Mmfile(File file, Mode mode) {
		if (vmIs64Bit())
			max_chunks_mapped = MAX_CHUNKS;
		this.mode = mode;
		switch (mode) {
		case CREATE :
			if (file.exists())
				verify(file.delete());
			break;
		case OPEN :
			if (!file.canRead() || !file.canWrite())
				throw new SuException("can't open " + file);
			break;
		case READ_ONLY:
			if (!file.canRead())
				throw new SuException("can't open " + file + " read-only");
			break;
		}
		try {
			fin = new RandomAccessFile(file, mode == Mode.READ_ONLY ? "r" : "rw");
		} catch (FileNotFoundException e) {
			throw new SuException("can't open or create " + file);
		}
		fc = fin.getChannel();
		if (mode != Mode.READ_ONLY) {
			try {
				FileLock lock = fc.tryLock();
				if (lock == null)
					throw new SuException("can't open " + file);
				lock.release();
			} catch (IOException e1) {
				throw new SuException("io exception locking " + file);
			}
		}
		try {
			file_size = fc.size();
		} catch (IOException e) {
			fatal("can't get database size");
		}
		verify(file_size >= 0);
		if (file_size == 0) {
			set_file_size(file_size = FILEHDR);
			buf(0).put(0, magic);
		} else {
			String err = checkfile();
			if (err != "")
				throw new SuException("not a valid database file (" + err + ")");
		}
	}

	private boolean vmIs64Bit() {
		return System.getProperty("java.vm.name").contains("64-Bit");
	}

	/** avoid memory mapping */
	private String checkfile() {
		try {
			byte[] m = new byte[4];
			fin.seek(0);
			fin.read(m);
			if (!Arrays.equals(m, magic))
				return "bad magic";
			if (file_size >= (long) MB_MAX_DB * 1024 * 1024)
				return "file too large " + file_size;

			fin.seek(FILESIZE_OFFSET);
			long saved_size = intToOffset(fin.readInt());
			if (saved_size > file_size)
				return "saved size > file size";
			// in case the file wasn't truncated last time
			file_size = Math.min(file_size, saved_size);
			if ((file_size % ALIGN) != 0)
				return "file size " + file_size + " not aligned";
			return "";
		} catch (IOException e) {
			return e.toString();
		}
	}

	// for testing only
	void set_chunk_size(int n) {
		chunk_size = n;
	}

	// used to reduce max_chunks_mapped
	// when accessing more than one database at the same time
	void set_max_chunks_mapped(int n) {
		verify(n >= 1);
		max_chunks_mapped = n;
	}

	@Override
	synchronized public void force() {
		try {
			for (int i = 0; i <= hi_chunk; ++i)
				if (fm[i] != null)
					fm[i].force();
		} catch (Exception e) {
			errlog("error from MappedByteBuffer.force: " + e);
		}
	}

	@Override
	synchronized public void close() {
		Arrays.fill(fm, null); // might help gc
		try {
			fc.close();
			fin.close();
		} catch (IOException e) {
			throw new SuException("can't close database file");
		}
		// should truncate file but probably can't
		// since memory mappings won't all be finalized
		// so file size will be rounded up to chunk size
		// this is handled when re-opening
	}

	public static int offsetToInt(long offset) {
		return (int) (offset >> SHIFT);
	}

	public static long intToOffset(int i) {
		return (i & 0xffffffffL) << SHIFT;
	}

	private long get_file_size() {
		return intToOffset(buf(FILESIZE_OFFSET).getInt(0));
	}

	private void set_file_size(long size) {
		verify((size % ALIGN) == 0);
		buf(FILESIZE_OFFSET).putInt(0, offsetToInt(size));
	}

	@Override
	public long size() {
		return file_size;
	}

	@Override
	synchronized public long alloc(int n, byte type) {
		verify(n < chunk_size);
		n = align(n);

		// if insufficient room in this chunk, advance to next
		// (by alloc'ing remainder)
		int chunk = (int) (file_size / chunk_size);
		int remaining = chunk_size - (int) (file_size % chunk_size);
		verify(remaining >= OVERHEAD);
		if (remaining < n + OVERHEAD) {
			verify(type != FILLER); // type 0 is filler, filler should always fit
			alloc(remaining - OVERHEAD, FILLER);
			verify(file_size / chunk_size == chunk + 1);
		}
		verify(type < ALIGN);
		long offset = file_size + HEADER;
		file_size += n + OVERHEAD;
		set_file_size(file_size);
		ByteBuf p = buf(offset - HEADER);
		p.putInt(0, n | type); // header
		p.putInt(HEADER + n, n ^ (int) (offset + n)); // trailer
		return offset;
	}

	private int align(int n) {
		return ((n - 1) | (ALIGN - 1)) + 1;
	}

	@Override
	synchronized public ByteBuf adr(long offset) {
		return buf(offset);
	}

	private ByteBuf buf(long offset) {
		verify(offset >= 0);
		verify(offset < file_size);
		int chunk = (int) (offset / chunk_size);
		verify(0 <= chunk && chunk < MAX_CHUNKS);
		if (fm[chunk] == null) {
			if (chunks_mapped >= max_chunks_mapped)
				evict_chunk();
			map(chunk);
			++chunks_mapped;
			if (chunk > hi_chunk)
				hi_chunk = chunk;
		}
		last_used[chunk] = ++clock;
		return ByteBuf.wrap(fm[chunk], (int) (offset % chunk_size));
	}

	private void map(int chunk) {
		verify(fm[chunk] == null);
		for (int tries = 0;; ++tries) {
			try {
				fm[chunk] = fc.map(
						mode == Mode.READ_ONLY ? FileChannel.MapMode.READ_ONLY
								: FileChannel.MapMode.READ_WRITE,
						(long) chunk * chunk_size, chunk_size);
				fm[chunk].order(ByteOrder.BIG_ENDIAN);
				return;
			} catch (IOException e) {
				if (tries > 10)
					fatal("can't map database file " + e);
				evict_chunk();
				System.gc();
				System.runFinalization();
			}
		}
	}

	private void evict_chunk() {
		unmap(lru_chunk());
		--chunks_mapped;
	}

	private int lru_chunk() {
		verify(chunks_mapped > 0);
		int min = 0;
		for (int i = 0; i < last_used.length; ++i)
			if (fm[i] != null && last_used[i] < last_used[min])
				min = i;
		return min;
	}

	private void unmap(int chunk) {
		fm[chunk].force();
		// have to depend on garbage collection finalization to unmap
		fm[chunk] = null;
	}

	private MmCheck check(long offset) {
		if (offset >= file_size + HEADER)
			return MmCheck.EOF;
		ByteBuf p = buf(offset - HEADER);
		int n = length(p);
		if (n > chunk_size)
			return MmCheck.ERR;
		// TODO check if off + n is in different chunk
		if (offset + n + TRAILER > file_size
				|| p.getInt(HEADER + n) != (n ^ (int) (offset + n)))
			return MmCheck.ERR;
		return MmCheck.OK;
	}

	@Override
	public int length(long offset) {
		return length(buf(offset - HEADER));
	}

	private static int length(ByteBuf bb) {
		return bb.getInt(0) & ~(ALIGN - 1);
	}

	@Override
	public byte type(long offset) {
		return type(buf(offset - HEADER));
	}

	private static byte type(ByteBuf bb) {
		return (byte) (bb.getInt(0) & (ALIGN - 1));
	}

	private long end_offset() {
		return file_size + HEADER;
	}

	@Override
	public long first() {
		return file_size <= FILEHDR ? 0 : FILEHDR + HEADER;
	}

	@Override
	public long last() {
		long offset = end_offset();
		if (offset <= FILEHDR + HEADER)
			return -1;
		offset -= OVERHEAD;
		int n = buf(offset).getInt(0) ^ (int) offset;
		if (n > chunk_size || n > offset)
			return -1;
		offset -= n;
		if (check(offset) == MmCheck.ERR)
			return -1;
		return offset;
	}

	/** avoids memory mapping */
	@Override
	public boolean checkEnd(byte type, byte value) {
		try {
			long offset = end_offset();
			if (offset <= FILEHDR + HEADER)
				return false;
			offset -= OVERHEAD;
			fin.seek(offset);
			int n = fin.readInt() ^ (int) offset;
			if (n > chunk_size || n > offset)
				return false;
			offset -= n + HEADER;
			fin.seek(offset);
			int t = fin.readInt() & (ALIGN - 1);
			byte v = fin.readByte();
			return t == type && v == value;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public Destination unwrap() {
		return this;
	}

	public MmfileIterator iterator() {
		return new MmfileIterator();
	}

	public MmfileIterator iterator(long start) {
		return new MmfileIterator(start);
	}

	public class MmfileIterator implements Iterator<ByteBuf> {
		private long offset = -1;
		private long next_offset = BEGIN_OFFSET;
		private boolean err = false;

		public MmfileIterator() {
		}

		public MmfileIterator(long start) {
			next_offset = start;
		}

		public boolean hasNext() {
			return next_offset < file_size;
		}

		public ByteBuf next() {
			do {
				offset = next_offset;
				next_offset += length() + OVERHEAD;
				switch (check(offset)) {
				case OK:
					break;
				case ERR:
					err = true;
					// fall thru
				case EOF:
					next_offset = last(); // eof or bad block
					return ByteBuf.empty();
				}
			} while (type() == FILLER);
			return adr(offset);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean corrupt() {
			return err;
		}

		public long offset() {
			return offset;
		}

		public byte type() {
			return Mmfile.this.type(offset);
		}

		public int length() {
			return Mmfile.this.length(offset);
		}
	}

}
