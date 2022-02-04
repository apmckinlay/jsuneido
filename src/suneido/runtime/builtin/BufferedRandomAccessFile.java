/* From: https://stackoverflow.com/questions/5614206/buffered-randomaccessfile-java
 * Therefore licensed under CC-SA 4
 * https://creativecommons.org/licenses/by/4.0/
 * With additional methods added.
 */

package suneido.runtime.builtin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * Adds caching to a random access file.
 *
 * Rather than directly writing down to disk or to the system which seems to be
 * what random access file/file channel do, add a small buffer and write/read
 * from it when possible. A single buffer is created, which means reads or
 * writes near each other will have a speed up. Read/writes that are not within
 * the cache block will not be speed up.
 *
 *
 */
public class BufferedRandomAccessFile implements AutoCloseable {

	private static final int DEFAULT_BUFSIZE = 4096;

	/**
	 * The wrapped random access file, we will hold a cache around it.
	 */
	private final RandomAccessFile raf;

	/**
	 * The size of the buffer
	 */
	private final int bufsize;

	/**
	 * The buffer.
	 */
	private final byte buf[];

	/**
	 * Current position in the file.
	 */
	private long pos = 0;

	/**
	 * When the buffer has been read, this tells us where in the file the buffer
	 * starts at.
	 */
	private long bufBlockStart = Long.MAX_VALUE;

	// Must be updated on write to the file
	private long actualFileLength = -1;

	boolean changeMadeToBuffer = false;

	// Must be update as we write to the buffer.
	private long virtualFileLength = -1;

	public BufferedRandomAccessFile(File name, String mode)
			throws FileNotFoundException {
		this(name, mode, DEFAULT_BUFSIZE);
	}

	/**
	 *
	 * @param file
	 * @param mode
	 *                 how to open the random access file.
	 * @param b
	 *                 size of the buffer
	 * @throws FileNotFoundException
	 */
	public BufferedRandomAccessFile(File file, String mode, int b)
			throws FileNotFoundException {
		this(new RandomAccessFile(file, mode), b);
	}

	public BufferedRandomAccessFile(RandomAccessFile raf)
			throws FileNotFoundException {
		this(raf, DEFAULT_BUFSIZE);
	}

	public BufferedRandomAccessFile(RandomAccessFile raf, int b) {
		this.raf = raf;
		try {
			this.actualFileLength = raf.length();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.virtualFileLength = actualFileLength;
		this.bufsize = b;
		this.buf = new byte[bufsize];
	}

	/**
	 * Sets the position of the byte at which the next read/write should occur.
	 *
	 * @param pos
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		this.pos = pos;
	}

	/**
	 * Sets the length of the file.
	 */
	public void setLength(long fileLength) throws IOException {
		this.raf.setLength(fileLength);
		if (fileLength < virtualFileLength) {
			virtualFileLength = fileLength;
		}
	}

	/**
	 * Writes the entire buffer to disk, if needed.
	 */
	private void writeBufferToDisk() throws IOException {
		if (!changeMadeToBuffer)
			return;
		int amountOfBufferToWrite = (int) Math.min((long) bufsize,
				virtualFileLength - bufBlockStart);
		if (amountOfBufferToWrite > 0) {
			raf.seek(bufBlockStart);
			raf.write(buf, 0, amountOfBufferToWrite);
			this.actualFileLength = virtualFileLength;
		}
		changeMadeToBuffer = false;
	}

	/**
	 * Flush the buffer to disk and force a sync.
	 */
	public void flush() throws IOException {
		writeBufferToDisk();
		this.raf.getChannel().force(false);
	}

	/**
	 * Based on pos, ensures that the buffer is one that contains pos
	 *
	 * After this call it will be safe to write to the buffer to update the byte
	 * at pos, if this returns true reading of the byte at pos will be valid as
	 * a previous write or set length has caused the file to be large enough to
	 * have a byte at pos.
	 *
	 * @return true if the buffer contains any data that may be read. Data may
	 *         be read so long as a write or the file has been set to a length
	 *         that us greater than the current position.
	 */
	private boolean readyBuffer() throws IOException {
		boolean isPosOutSideOfBuffer = pos < bufBlockStart
				|| bufBlockStart + bufsize <= pos;

		if (isPosOutSideOfBuffer) {

			writeBufferToDisk();

			// The buffer is always positioned to start at a multiple of a
			// bufsize offset.
			// e.g. for a buf size of 4 the starting positions of buffers can be
			// at 0, 4, 8, 12..
			// Work out where the buffer block should start for the given
			// position.
			long bufferBlockStart = (pos / bufsize) * bufsize;

			assert bufferBlockStart >= 0;

			// If the file is large enough, read it into the buffer.
			// if the file is not large enough we have nothing to read into the
			// buffer,
			// In both cases the buffer will be ready to have writes made to it.
			if (bufferBlockStart < actualFileLength) {
				raf.seek(bufferBlockStart);
				raf.read(buf);
			}

			bufBlockStart = bufferBlockStart;
		}

		return pos < virtualFileLength;
	}

	/**
	 * Reads a byte from the file, returning an integer of 0-255, or -1 if it
	 * has reached the end of the file.
	 *
	 * @return
	 * @throws IOException
	 */
	public int read() throws IOException {
		if (readyBuffer() == false) {
			return -1;
		}
		try {
			return (buf[(int) (pos - bufBlockStart)]) & 0x000000ff;
		} finally {
			pos++;
		}
	}

	/**
	 * Write a single byte to the file.
	 *
	 * @param b
	 * @throws IOException
	 */
	public void write(byte b) throws IOException {
		readyBuffer(); // ignore result we don't care.
		buf[(int) (pos - bufBlockStart)] = b;
		changeMadeToBuffer = true;
		pos++;
		if (pos > virtualFileLength) {
			virtualFileLength = pos;
		}
	}

	/**
	 * Write all given bytes to the random access file at the current possition.
	 *
	 */
	public void write(byte[] bytes) throws IOException {
		int writen = 0;
		int bytesToWrite = bytes.length;
		{
			readyBuffer();
			int startPositionInBuffer = (int) (pos - bufBlockStart);
			int lengthToWriteToBuffer = Math.min(bytesToWrite - writen,
					bufsize - startPositionInBuffer);
			assert startPositionInBuffer + lengthToWriteToBuffer <= bufsize;

			System.arraycopy(bytes, writen, buf, startPositionInBuffer,
					lengthToWriteToBuffer);
			pos += lengthToWriteToBuffer;
			if (pos > virtualFileLength) {
				virtualFileLength = pos;
			}
			writen += lengthToWriteToBuffer;
			this.changeMadeToBuffer = true;
		}

		// Just write the rest to the random access file
		if (writen < bytesToWrite) {
			writeBufferToDisk();
			int toWrite = bytesToWrite - writen;
			raf.write(bytes, writen, toWrite);
			pos += toWrite;
			if (pos > virtualFileLength) {
				virtualFileLength = pos;
				actualFileLength = virtualFileLength;
			}
		}
	}

	/**
	 * Read up to to the size of bytes,
	 *
	 * @return the number of bytes read.
	 */
	public int read(byte[] bytes) throws IOException {
		int read = 0;
		int bytesToRead = bytes.length;
		while (read < bytesToRead) {

			// First see if we need to fill the cache
			if (readyBuffer() == false) {
				// No more to read;
				return read;
			}

			// Now read as much as we can (or need from cache and place it
			// in the given byte[]
			int startPositionInBuffer = (int) (pos - bufBlockStart);
			int lengthToReadFromBuffer = Math.min(bytesToRead - read,
					bufsize - startPositionInBuffer);

			System.arraycopy(buf, startPositionInBuffer, bytes, read,
					lengthToReadFromBuffer);

			pos += lengthToReadFromBuffer;
			read += lengthToReadFromBuffer;
		}

		return read;
	}

	public void close() throws IOException {
		try {
			this.writeBufferToDisk();
		} finally {
			raf.close();
		}
	}

	/**
	 * Gets the length of the file.
	 *
	 * @return
	 * @throws IOException
	 */
	public long length() throws IOException {
		return virtualFileLength;
	}

	// ADDITIONS ----------------------------------------------------

	public FileChannel getChannel() throws IOException {
		return raf.getChannel();
	}

	public long getFilePointer() throws IOException {
		return pos;
	}

	@SuppressWarnings("deprecation")
	public void writeBytes(String s) throws IOException {
		int len = s.length();
		byte[] b = new byte[len];
		s.getBytes(0, len, b, 0);
		write(b);
	}

	public void force() throws IOException {
		writeBufferToDisk();
		raf.getChannel().force(true);
	}

}
