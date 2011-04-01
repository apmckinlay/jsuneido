/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.*;
import java.nio.channels.FileChannel;

import suneido.SuException;

public class FileUtils {

	public static void copy(String sourceFile, String destFile)
			throws IOException {
		copy(sourceFile, destFile, new File(sourceFile).length());
	}

	public static void copy(String sourceFile, String destFile, long length)
			throws IOException {
		copy(new File(sourceFile), new File(destFile), length);
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
		try {
			File tmpfile = File.createTempFile("sudb", null, new File("."));
			tmpfile.deleteOnExit();
			return tmpfile;
		} catch (IOException e) {
			throw new SuException("Can't create temp file", e);
		}
	}

	public static void renameWithBackup(File tmpfile, String filename) {
		File file = new File(filename);
		File bakfile = new File(filename + ".bak");
		if (bakfile.exists() && !bakfile.delete())
			throw new SuException("can't delete " + bakfile);
		if (file.exists() && !file.renameTo(bakfile))
			throw new SuException("can't rename " + file + " to " + bakfile);
		if (!tmpfile.renameTo(file)) {
			bakfile.renameTo(file);
			throw new SuException("can't rename " + tmpfile + " to " + file);
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
