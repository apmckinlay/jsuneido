/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;

import suneido.util.FileUtils;

public class DbRebuild {

	public static void rebuildOrExit(String dbfilename) {
		File tempfile = FileUtils.tempfile();
		if (! rebuild(dbfilename, tempfile.getPath()))
			System.exit(-1);
	}

	public static boolean rebuild(String dbfilename, String tempfilename) {
		long okSize = check(dbfilename);
		try {
			Fix.fix(dbfilename, okSize);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static long check(String dbfilename) {
		Storage stor = new MmapFile(dbfilename, "r");
		Check check = new Check(stor);
		check.fullcheck();
		stor.close();
		return check.okSize();
	}

	public static void main(String[] args) {
		rebuildOrExit("immu.db");
	}

}
