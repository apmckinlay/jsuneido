/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import suneido.DbTools;
import suneido.util.FileUtils;

public class DbRebuild {

	public static String rebuild(String dbFilename, String tempFilename) {
		Check check = check(dbFilename);
		try {
			fix(dbFilename, tempFilename, check.okSize());
			return "Last commit " +
					new SimpleDateFormat("yyyy-MM-dd HH:mm").format(check.lastOkDatetime());
		} catch (Exception e) {
			return null;
		}
	}

	private static Check check(String dbFilename) {
		Storage stor = new MmapFile(dbFilename, "r");
		try {
			Check check = new Check(stor);
			check.fullcheck();
			return check;
		} finally {
			stor.close();
		}
	}

	/** Fixing is easy - just copy the good prefix of the file */
	static void fix(String filename, String tempFilename, long okSize) {
		try {
			FileUtils.copy(new File(filename), new File(tempFilename), okSize);
		} catch (IOException e) {
			throw new RuntimeException("Rebuild copy failed", e);
		}
	}

	public static void main(String[] args) {
		DbTools.rebuildOrExit(DatabasePackage.dbpkg, "immu.db");
	}

}
