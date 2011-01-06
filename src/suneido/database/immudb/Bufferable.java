/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

public interface Bufferable {

	int nBufferable();

	/** @return the number of lengths added */
	int lengths(int[] lengths, int at);

	/** Multiple fields must be added in reverse order */
	void addTo(ByteBuffer buf);

}
