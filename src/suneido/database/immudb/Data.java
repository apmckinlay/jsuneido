/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

/**
 * Record field values.
 */
public interface Data extends Comparable<Data> {
	public abstract int length();

	public abstract void addTo(ByteBuffer buf);

	public abstract byte[] asArray();

	public abstract byte byteAt(int i);

}
