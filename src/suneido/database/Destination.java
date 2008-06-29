package suneido.database;

import java.nio.ByteBuffer;

/**
 * Interface from {@link Btree} and {@link BtreeIndex} to {@link Database}.
 * Normally implemented by {@link Database} but {@link DestMem} is used for
 * tests and in-memory temporary indexes.
 * 
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public interface Destination {

	long alloc(int size);

	ByteBuffer adr(long offset);

	long size();

	TranRead read_act(int tran, int tblnum, String index);

	boolean visible(int tran, long adr);

	Record input(long adr);

}
