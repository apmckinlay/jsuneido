/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.io.IOException;

import suneido.util.FileUtils;

class Fix {

	/** Fixing is easy - just copy the good prefix of the file */
	static void fix(String filename, long okSize) {
		File tempfile = FileUtils.tempfile();
		try {
			FileUtils.copy(new File(filename), tempfile, okSize);
		} catch (IOException e) {
			throw new RuntimeException("fix copy failed", e);
		}
		FileUtils.renameWithBackup(tempfile, filename);
	}

}
