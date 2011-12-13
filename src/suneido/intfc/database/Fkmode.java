/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

public class Fkmode {
	public static final int
		BLOCK = 0,
		CASCADE_UPDATES = 1,
		CASCADE_DELETES = 2,
		CASCADE = 3;

	public static String toString(int mode) {
		switch (mode) {
		case Fkmode.BLOCK: return "block";
		case Fkmode.CASCADE: return "cascade";
		case Fkmode.CASCADE_DELETES: return "cascade deletes";
		case Fkmode.CASCADE_UPDATES: return "cascade updates";
		default: throw new RuntimeException("unknown fkmode " + mode);
		}
	}

}
