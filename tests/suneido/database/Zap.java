/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import suneido.util.ByteBuf;

/**
 * corrupt the end of the file
 * so it appears the database was not shut down properly
 */
public class Zap {

	public static void main(String[] args) {
		Mmfile mmf = new Mmfile("suneido.db", Mode.OPEN);
		long size = mmf.size();
		ByteBuf buf = mmf.adr(size - 8);
		buf.putLong(0, ~0);
		mmf.close();
		System.out.println("zapped");
	}

}
