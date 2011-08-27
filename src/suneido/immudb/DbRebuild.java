/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;


public class DbRebuild {

	public static void rebuildOrExit(String dbfilename) {
		long okSize = check(dbfilename);
		Fix.fix(dbfilename, okSize);
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
