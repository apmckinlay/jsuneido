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
public interface Destination {

	long alloc(int size, byte type);

	ByteBuf adr(long offset);

	ByteBuf adrForWrite(long offset);

	long first();

	int length(long adr);

	long size();

	void sync();

	void close();

	Destination unwrap();

}
