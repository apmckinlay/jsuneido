/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;

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

	@Override
	public suneido.Record record() {
		return new Record();
	}

	@Override
	public suneido.Record record(int size) {
		return new Record(size);
	}

	@Override
	public suneido.Record record(ByteBuffer buf) {
		return new Record(buf);
	}

}
