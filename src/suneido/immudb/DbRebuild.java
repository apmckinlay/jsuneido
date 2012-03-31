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
		Check2 check = check(dbFilename);
		try {
			fix(dbFilename, tempFilename, check.dOkSize, check.iOkSize);
			return "Last commit " +
					new SimpleDateFormat("yyyy-MM-dd HH:mm").format(check.lastOkDatetime());
		} catch (Exception e) {
			return null;
		}
	}

	private static Check2 check(String dbFilename) {
		Storage dstor = new MmapFile(dbFilename + "d", "r");
		Storage istor = new MmapFile(dbFilename + "i", "r");
		try {
			Check2 check = new Check2(dstor, istor);
			check.fullcheck();
			return check;
		} finally {
			dstor.close();
			istor.close();
		}
	}

	/** Fixing is easy - just copy the good prefix of the file */
	static void fix(String filename, String tempFilename, long dOkSize, long iOkSize) {
		try {
			FileUtils.copy(new File(filename), new File(tempFilename), dOkSize);
			//TODO copy index file
			//TODO reprocess data after last good persist
		} catch (IOException e) {
			throw new RuntimeException("Rebuild copy failed", e);
		}
	}

	public static void main(String[] args) {
		DbTools.rebuildOrExit(DatabasePackage2.dbpkg, "immu.db");
	}

}
