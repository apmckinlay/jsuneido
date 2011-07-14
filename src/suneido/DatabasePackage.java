/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.nio.ByteBuffer;


public interface DatabasePackage {

	DatabaseIntfc open(String filename);

	DatabaseIntfc testdb();

	int offsetToInt(long offset);

	long intToOffset(int i);

	Record record();
	Record record(int size);
	Record record(ByteBuffer buf);

}
