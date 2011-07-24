/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.nio.ByteBuffer;

import suneido.Suneido;

public interface Record
		extends suneido.Packable, Comparable<Record>, Iterable<ByteBuffer> {

	Record MINREC = Suneido.dbpkg.minRecord();
	Record MAXREC = Suneido.dbpkg.maxRecord();
	ByteBuffer MIN_FIELD = ByteBuffer.allocate(0);
	ByteBuffer MAX_FIELD = ByteBuffer.allocate(1).put(0, (byte) 0x7f).asReadOnlyBuffer();

	Record squeeze();

	ByteBuffer getBuffer();

	ByteBuffer getRaw(int i);
	String getString(int i);
	int getInt(int i);
	Object get(int i);

	/**
	 * @return The number of fields in the Record.
	 */
	int size();

	boolean isEmpty();

	/**
	 * @return The current buffer size. May be larger than packSize.
	 */
	int bufSize();

	/**
	 * @return The minimum size the current data would fit into.
	 * 		   <b>Note:</b> This may be smaller than the current buffer size.
	 */
	int packSize();

	Object getRef();

	/**
	 * Used for temp indexes and for updating
	 */
	int address();

}