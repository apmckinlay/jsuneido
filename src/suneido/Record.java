/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.nio.ByteBuffer;

public interface Record
		extends suneido.Packable, Comparable<Record>, Iterable<ByteBuffer> {

	long off();

	Record dup();

	Record dup(int extra);

	Record add(ByteBuffer src);

	Record add(Object x);

	Record addMin();

	Record addMax();

	ByteBuffer getBuffer();

	ByteBuffer getraw(int i);

	Object get(int i);

	String getString(int i);

	int getInt(int i);

	Record truncate(int n);

	/**
	 * @return The number of fields in the BufRecord.
	 */
	int size();

	boolean isEmpty();

	/**
	 * @return The current buffer size. May be larger than the packsize.
	 */
	int bufSize();

	/**
	 * @return The minimum size the current data would fit into. <b>Note:</b>
	 *         This may be smaller than the current buffer size.
	 */
	int packSize();

	Object getRef();

}