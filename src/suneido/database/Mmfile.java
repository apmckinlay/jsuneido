package suneido.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Iterator;

import suneido.SuException;

import static suneido.Suneido.fatal;
import static suneido.Suneido.verify;

/**
 * Memory mapped file access using Java NIO.
 * Maps in 4mb chunks.
 * A maximum of 1gb is mapped at any one time.
 * Chunks are unmapped as necessary roughly LRU using a clock method.
 * Individual blocks must fit in a single chunk.
 * Blocks are aligned on 8 byte boundaries
 * allowing offsets to be shifted right to fit in 32 bit int's.
 * Alignment also allows storing a type in the low bits of the block size.
 * Since offsets are stored shifted as int's
 * maximum file size is 32gb (max unsigned int (4gb) << 3). * 
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Mmfile implements Iterable<ByteBuffer> {
	final static int HEADER = 4;	// contains size | type
	final static int TRAILER = 4;	// contains size ^ adr
	final static int OVERHEAD = HEADER + TRAILER;
	/**
	 * ALLIGN must be a power of 2
	 * and big enough to allow space for types
	 * e.g. align of 8 = 3 bits for type = 8 types
	 */
	final public static int ALIGN = 8;
	final public static int SHIFT = 2;
	final private static int MB_PER_CHUNK = 4;
	final private static int MAX_CHUNKS_MAPPED = 1024 / MB_PER_CHUNK;
	final private static int MB_MAX_DB = 32 * 1024; // 32 gb
	final private static int MAX_CHUNKS = MB_MAX_DB / MB_PER_CHUNK;
	final private static int FILEHDR = 8; // should be multiple of align
	final private static byte[] magic = new byte[] { 'S', 'n', 'd', 'o' };
	final private static int FILESIZE_OFFSET = 4;
	final private static int BEGIN_OFFSET = FILEHDR + HEADER;
	final private static byte FILLER = 0;
	final public static byte DATA = 1;
	final public static byte COMMIT = 2;
	final public static byte SESSION = 3;
	final public static byte OTHER = 4;
	private static enum MmCheck { OK, ERR, EOF };
	
	// these are only overridden for tests
	private int chunk_size = MB_PER_CHUNK * 1024 * 1024;
	private int max_chunks_mapped = MAX_CHUNKS_MAPPED;
	
	private RandomAccessFile fin;
	private FileChannel fc;
	private long file_size;
	private MappedByteBuffer[] fm = new MappedByteBuffer[MAX_CHUNKS];
	private boolean[] recently_used = new boolean[MAX_CHUNKS];
	private int chunks_mapped = 0;
	private int hi_chunk = 0;
	private int hand = 0;
	private int last_alloc = 0;
	
	public Mmfile(String filename, Mode mode) {
		File file = new File(filename);
		if (mode == Mode.OPEN && (! file.canRead() || ! file.canWrite()))
			throw new SuException("can't open " + filename);
		try {
			fin = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new SuException("can't open or create " + filename);
		}
		fc = fin.getChannel();
		try {
			file_size = fc.size();
		} catch (IOException e) {
			fatal("can't get database size");
		}
		verify(file_size >= 0);
		if (file_size == 0) {
			set_file_size(file_size = FILEHDR);
			buf(0).put(magic);
		} else {
			String err = checkfile();
			if (err != "")
				throw new SuException("not a valid database file (" + err + ")");
		}
	}
	private String checkfile() {
		byte[] buf = new byte[4];
		buf(0).get(buf);
		if (! Arrays.equals(buf, magic))
			return "bad magic";
		if (file_size >= (long) MB_MAX_DB * 1024 * 1024)
			return "file too large " + file_size;
		
		long saved_size = get_file_size();
		if (saved_size > file_size)
			return "saved size > file size";
		// in case the file wasn't truncated last time
		file_size = Math.min(file_size, saved_size);
		if ((file_size % ALIGN) != 0)
			return "file size " + file_size + " not aligned";
		return "";
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

	public void force() {
		for (int i = 0; i <= hi_chunk; ++i)
			if (fm[i] != null)
				fm[i].force();
	}

	public void close() {
		Arrays.fill(fm, null); // might help gc
		try {
			fc.close();
		} catch (IOException e) {
			throw new SuException("can't close database file");
		}
		fc = null;
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
		return intToOffset(buf(FILESIZE_OFFSET).getInt());
	}
	private void set_file_size(long size) {
		verify((size % ALIGN) == 0);
		buf(FILESIZE_OFFSET).putInt(offsetToInt(size));
	}
	
	public long size() {
		return file_size;
	}
	
	public long alloc(int n, byte type) {
		verify(n < chunk_size);
		last_alloc = n;
		n = align(n);
	
		// if insufficient room in this chunk, advance to next
		// (by alloc'ing remainder) 
		int chunk = (int) (file_size / chunk_size);
		int remaining = chunk_size - (int) (file_size % chunk_size);
		verify(remaining >= OVERHEAD);
		if (remaining < n + OVERHEAD)
			{
			verify(type != FILLER); // type 0 is filler, filler should always fit
			alloc(remaining - OVERHEAD, FILLER);
			verify(file_size / chunk_size == chunk + 1);
			}
		verify(type < ALIGN);
		long offset = file_size + HEADER;
		file_size += n + OVERHEAD;
		set_file_size(file_size);
		ByteBuffer p = buf(offset - HEADER);
		p.putInt(0, n | type); // header
		p.putInt(HEADER + n, n ^ (int) (offset + n)); // trailer
		return offset;
	}
	private int align(int n) {
		return ((n - 1) | (ALIGN - 1)) + 1;
	}
	
	public void unalloc(int n) {
		verify(n == last_alloc);
		n = align(n);
		file_size -= n + OVERHEAD;
		set_file_size(file_size);
	}

	public ByteBuffer adr(long offset) {
		ByteBuffer buf = buf(offset);
//		buf.limit(length(offset));
		return buf;
	}
	private ByteBuffer buf(long offset) {
		verify(offset >= 0);
		verify(offset < file_size);
		int chunk = (int) (offset / chunk_size);
		verify(0 <= chunk && chunk < MAX_CHUNKS);
		if (fm[chunk] == null)
			{
			if (chunks_mapped >= max_chunks_mapped)
				evict_chunk();
			map(chunk);
			++chunks_mapped;
			if (chunk > hi_chunk)
				hi_chunk = chunk;
			}
		recently_used[chunk] = true;
		fm[chunk].position((int) (offset % chunk_size));
		return fm[chunk].slice();
	}
	
	private void map(int chunk) {
		verify(fm[chunk] == null);
		for (int tries = 0; ; ++tries) {		
			try {
				fm[chunk] = fc.map(FileChannel.MapMode.READ_WRITE, (long) chunk * chunk_size, chunk_size);
				return ;
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
		// the *2 is to allow for two passes
		// first to clear recently_used, second to find it
		for (int i = 0; i < 2 * MAX_CHUNKS; ++i)
			{
			hand = (hand + 1) % MAX_CHUNKS;
			if (fm[hand] == null)
				continue ;
			if (! recently_used[hand])
				return hand;
			recently_used[hand] = false;
			}
		throw SuException.unreachable();
	}

	private void unmap(int chunk) {
		// have to depend on garbage collection finalization to unmap
		fm[chunk] = null;
	}
	
	private MmCheck check(long offset) {
		if (offset >= file_size + HEADER)
			return MmCheck.EOF;
		ByteBuffer p = buf(offset - HEADER);
		int n = length(p);
		if (n > chunk_size)
			return MmCheck.ERR;
		// TODO check if off + n is in different chunk
		if (offset + n + TRAILER > file_size ||
			p.getInt(HEADER + n) != (n ^ (int) (offset + n)))
			return MmCheck.ERR;
		return MmCheck.OK;
	}
	public int length(long offset) {
		return length(buf(offset - HEADER));
	}
	private int length(ByteBuffer bb) {
		return bb.getInt(0)& ~(ALIGN - 1);
	}
	private byte type(long offset) {
		return type(buf(offset - HEADER));
	}
	private byte type(ByteBuffer bb) {
		return (byte) (bb.getInt(0) & (ALIGN - 1));
	}
	private long end_offset() {
		return file_size + HEADER;
	}
	
	long first() {
		return file_size <= FILEHDR ? 0 : FILEHDR + HEADER;
	}

	
	public Iterator<ByteBuffer> iterator() {
		return new MmfileIterator();
	}
	private class MmfileIterator implements Iterator<ByteBuffer> {
		private long offset = BEGIN_OFFSET;
		private boolean err = false;
		
		public boolean hasNext() {
			return offset < file_size;
		}

		public ByteBuffer next() {
			long p;
			do {
				p = offset;
				offset += length(p) + OVERHEAD;
				switch (check(p)) {
				case OK :
					break ;
				case ERR :
					err = true;
					// fall thru
				case EOF :
					offset = end_offset(); // eof or bad block
					return ByteBuffer.allocate(0);
				}
			} while (type(p) == FILLER);
		return adr(p);
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
	}

	public Iterator<ByteBuffer> reverse_iterator() {
		return new MmfileReverseIterator();
	}
	private class MmfileReverseIterator implements Iterator<ByteBuffer> {
		private long offset = end_offset();
		private boolean err = false;
		
		public boolean hasNext() {
			return offset > FILEHDR + OVERHEAD;
		}

		public ByteBuffer next() {
			do {
				offset -= OVERHEAD;
				int n = buf(offset).getInt() ^ (int) offset;
				if (n > chunk_size || n > offset) {
					err = true;
					offset = BEGIN_OFFSET;
					return ByteBuffer.allocate(0);
				}
				offset -= n;
				switch (check(offset)) {
				case OK :
					break ;
				case ERR :
					err = true;
					// fall thru
				case EOF :
					offset = BEGIN_OFFSET; // eof or bad block
					return ByteBuffer.allocate(0);
				}
			} while (type(offset) == FILLER);
		return adr(offset);
		}
		public void remove() {
			throw SuException.unreachable();
		}
		public boolean corrupt() {
			return err;
		}
		public long offset() {
			return offset;
		}
	}
	
//    public static void main(String[] args) throws Exception {
//    	RandomAccessFile f = new RandomAccessFile("tmp", "rw");
//    	f.seek(3L * 1024 * 1024 * 1024);
//    	f.write(123);
//        FileChannel channel = f.getChannel();
//        long size = channel.size();
//        System.out.println("File is " + size + " bytes large");
//        for (int i = 0; i < 10; ++i) {
//	        long ofs = 0;
//	        int chunksize = 512 * 1024 * 1024;
//	        while (ofs < size) {
//	            int n = (int)Math.min(chunksize, size - ofs);
//	            int tries = 0;
//	            ByteBuffer buffer = null;
//	            do {
//		            try {
//		            	buffer = channel.map(FileChannel.MapMode.READ_ONLY, ofs, n);
//		            } catch (IOException e) {
//		            	if (++tries > 10)
//		            		throw e;
//		            	System.gc();
//		            	System.runFinalization();
//		            }
//	            } while (buffer == null);
//	            System.out.println("Mapped " + n + " bytes at offset " + ofs + " with " + tries + " tries");
//	            ofs += n;
//	            buffer = null; // "unmap"
//	        }
//        }
//        channel.close();
//    }
}
