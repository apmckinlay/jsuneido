/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import suneido.database.Database;

public interface DatabasePackage {

	Database open(String filename);

	Database testdb();

	int offsetToInt(long offset);

	long intToOffset(int i);

}
