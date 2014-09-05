/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static suneido.util.Verify.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import suneido.SuException;

public class FileUtils {

	public static void copy(String sourceFile, String destFile)
			throws IOException {
		copy(new File(sourceFile), new File(destFile));
	}

	public static void copy(String sourceFile, String destFile, long length)
			throws IOException {
		copy(new File(sourceFile), new File(destFile), length);
	}

	public static void copy(File sourceFile, File destFile) throws IOException {
		copy(sourceFile, destFile, sourceFile.length());
	}

	public static void copy(File sourceFile, File destFile, long length)
			throws IOException {
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		FileChannel source = null;
		FileChannel destination = null;
		try {
			inputStream = new FileInputStream(sourceFile);
			source = inputStream.getChannel();
			outputStream = new FileOutputStream(destFile);
			destination = outputStream.getChannel();
			for (long pos = 0; pos < length; ) {
				 long nread = destination.transferFrom(source, pos, length - pos);
				 if (nread == 0)
					 throw new IOException("FileCopy could not read source");
				 pos += nread;
			}
		} finally {
			if(source != null)
				source.close();
			if(destination != null)
				destination.close();
			if (inputStream != null)
				inputStream.close();
			if (outputStream != null)
				outputStream.close();
		}
	}

	public static File tempfile() {
		return tempfile(null);
	}

	public static File tempfile(String suffix) {
		try {
			File tmpfile = File.createTempFile("sutmp", suffix, new File("."));
			tmpfile.deleteOnExit();
			return tmpfile;
		} catch (IOException e) {
			throw new RuntimeException("Can't create temp file", e);
		}
	}

	public static void deleteIfExisting(String filename) {
		File file = new File(filename);
		if (file.exists())
			verify(file.delete(), "delete failed " + filename);
	}

	/** NOTE: assumes starting at beginning of buffer */
	public static void fullRead(ReadableByteChannel in, ByteBuffer buf, int n)
			throws IOException {
		buf.limit(n);
		do {
			if (in.read(buf) < 1)
				throw new SuException("premature eof in fullRead " + n);
		} while (buf.hasRemaining());
		buf.position(0);
	}

	public static String getline(ReadableByteChannel in) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1);
		StringBuilder sb = new StringBuilder();
		while (true) {
			buf.rewind();
			if (in.read(buf) != 1)
				return null;
			char c = (char) buf.get(0);
			if (c == '\n')
				break;
			sb.append(c);
		}
		return sb.toString();
	}

	public static int readInt(ReadableByteChannel in, ByteBuffer buf)
			throws IOException {
		buf.rewind();
		verify(in.read(buf) == 4);
		return buf.getInt(0);
	}

	public static void renameWithBackup(String tmpfile, String filename) {
		renameWithBackup(new File(tmpfile), new File(filename));
	}

	public static void renameWithBackup(File tmpfile, File file) {
		File bakfile = new File(file + ".bak");
		if (bakfile.exists() && ! bakfile.delete())
			throw new RuntimeException("can't delete " + bakfile);
		if (file.exists() && ! file.renameTo(bakfile))
			throw new RuntimeException("can't rename " + file + " to " + bakfile);
		if (! tmpfile.renameTo(file)) {
			bakfile.renameTo(file);
			throw new RuntimeException("can't rename " + tmpfile + " to " + file);
		}
	}

	public static void main(String[] args) {
		try {
			copy("../suneido.db", "../suneido.db.copy");
			System.out.println("copied successfully");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
