/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;
import suneido.language.Ops;
import suneido.language.Params;

public class DeleteDir {

	@Params("string")
	public static Boolean DeleteDir(Object d) {
		String path = Ops.toStr(d);
		File dir = new File(path);
		if (!dir.isDirectory())
			return false;
		try {
			deleteRecursively(dir);
		} catch (IOException e) {
			throw new SuException("DeleteDir failed", e);
		}
		return true;
	}

	private static void deleteRecursively(File file) throws IOException {
		if (file.isDirectory())
			deleteDirectoryContents(file);
		if (!file.delete())
			throw new IOException("Failed to delete " + file);
	}

	// can't use Guava version because it can fail sometimes
	// when !directory.getCanonicalPath().equals(directory.getAbsolutePath())
	private static void deleteDirectoryContents(File directory)
			throws IOException {
		File[] files = directory.listFiles();
		if (files == null)
			throw new IOException("Error listing files for " + directory);
		for (File file : files)
			deleteRecursively(file);
	}

}
