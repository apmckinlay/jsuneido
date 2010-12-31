/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import javax.annotation.concurrent.Immutable;

import suneido.util.ByteBuf;

@Immutable
public class DbRecord implements Record {
	@SuppressWarnings("unused")
        private final ByteBuf buf;

	public DbRecord(ByteBuf buf) {
		this.buf = buf;
	}

	@Override
	public void add(ByteBuf buf) {
		throw new UnsupportedOperationException("DbRecord.add");
	}

	@Override
	public ByteBuf get(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
