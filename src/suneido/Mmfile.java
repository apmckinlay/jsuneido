package suneido;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static suneido.Suneido.fatal;
import static suneido.Suneido.verify;

public class Mmfile {
	final static int HEADER = 4;	// size | type
	final static int TRAILER = 4;	// size ^ adr
	final static int OVERHEAD = HEADER + TRAILER;
	/**
	 * must be a power of 2
	 * must be big enough to allow space for types
	 * e.g. align of 8 = 3 bits for type = 8 types
	 */
	final static int ALIGN = 8;
	final static int MB_PER_CHUNK = 4;
	final static int MAX_CHUNKS_MAPPED = 1024 / MB_PER_CHUNK;
	final static int SHIFT = 2;
	final static int MB_MAX_DB = 16 * 1024; // 16 gb
	final static int MAX_CHUNKS = MB_MAX_DB / MB_PER_CHUNK;
	final static int FILEHDR = 8; // should be multiple of align
	final byte[] magic = new byte[] { 'S', 'n', 'd', 'o' };
	
	// these are only overridden for tests
	int chunk_size = MB_PER_CHUNK * 1024 * 1024;
	int max_chunks_mapped = MAX_CHUNKS_MAPPED;
	
	RandomAccessFile fin;
	FileChannel fc;
	long file_size;
	MappedByteBuffer[] fm = new MappedByteBuffer[MAX_CHUNKS];
	boolean[] recently_used = new boolean[MAX_CHUNKS];
	int chunks_mapped = 0;
	int hi_chunk = 0;
	int hand = 0;
	
	Mmfile(String filename) {
		this(filename, false);
	}
	Mmfile(String filename, boolean create) {
		File file = new File(filename);
		if (! create && (! file.canRead() || ! file.canWrite()))
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
		verify(file_size < (long) MB_MAX_DB * 1024 * 1024);
		verify((file_size % ALIGN) == 0);
		if (file_size == 0) {
			set_file_size(file_size = FILEHDR);
			adr(0).put(magic);
		} else {
			if (0 != memcmp(adr(0), magic, magic.length))
				throw new SuException("not a valid database file (or old version)");
			long saved_size = get_file_size();
			// in case the file wasn't truncated last time
			file_size = Math.min(file_size, saved_size);
		}
	}
	
	long get_file_size() {
		return 0; // TODO
	}
	void set_file_size(long size) {
		// TODO
	}
	
	ByteBuffer adr(long offset) 	{
		verify(offset >= 0);
		verify(offset < file_size);
		int chunk = (int) (offset / chunk_size);
		verify(chunk < MAX_CHUNKS);
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
		for (int tries = 0; tries < 10; ++tries) {		
			try {
				fm[chunk] = fc.map(FileChannel.MapMode.READ_WRITE, chunk * chunk_size, chunk_size);
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
			if (! base[hand])
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

    public static void main(String[] args) throws Exception {
    	RandomAccessFile f = new RandomAccessFile("tmp", "rw");
    	f.seek(5L * 1024 * 1024 * 1024);
    	f.write(123);
        FileChannel channel = f.getChannel();
        long size = channel.size();
        System.out.println("File is " + size + " bytes large");
        for (int i = 0; i < 10; ++i) {
	        long ofs = 0;
	        int chunksize = 512 * 1024 * 1024;
	        while (ofs < size) {
	            int n = (int)Math.min(chunksize, size - ofs);
	            int tries = 0;
	            ByteBuffer buffer = null;
	            do {
		            try {
		            	buffer = channel.map(FileChannel.MapMode.READ_ONLY, ofs, n);
		            } catch (IOException e) {
		            	if (++tries > 10)
		            		throw e;
		            	System.gc();
		            	System.runFinalization();
		            }
	            } while (buffer == null);
	            System.out.println("Mapped " + n + " bytes at offset " + ofs + " with " + tries + " tries");
	            ofs += n;
	            buffer = null; // "unmap"
	        }
        }
        channel.close();
    }
}
