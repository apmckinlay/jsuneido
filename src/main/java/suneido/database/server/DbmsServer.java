/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

// this allows easily swapping implementations
public class DbmsServer extends DbmsServerBinary {

	public DbmsServer(int timeoutMin) {
		super(timeoutMin);
	}

}
