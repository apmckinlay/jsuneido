/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

public class DatabasePackage implements suneido.DatabasePackage {

	@Override
	public Database open(String filename) {
		return new Database(filename, Mode.OPEN);
	}

	@Override
	public int offsetToInt(long offset) {
		return Mmfile.offsetToInt(offset);
	}

	@Override
	public long intToOffset(int i) {
		return Mmfile.intToOffset(i);
	}

	@Override
	public Database testdb() {
		return new Database(new DestMem(), Mode.CREATE);
	}

}
