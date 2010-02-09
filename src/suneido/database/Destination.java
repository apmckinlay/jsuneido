package suneido.database;

import suneido.util.ByteBuf;

/**
 * Interface from {@link Database}, {@link BtreeIndex}, {@link Btree) to storage.
 * Normally implemented by {@link Mmfile} but {@link DestMem} is used for
 * tests and in-memory temporary indexes.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class Destination {

	public abstract long alloc(int size, byte type);

	abstract ByteBuf adr(long offset);

	ByteBuf node(long offset) {
		return adr(offset);
	}

	ByteBuf nodeForWrite(long offset) {
		return adr(offset);
	}

	abstract long first();

	abstract long last();

	abstract int length(long adr);

	abstract byte type(long adr);

	abstract long size();

	abstract void sync();

	abstract void close();

	abstract Destination unwrap();

}
