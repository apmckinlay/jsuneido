/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

//this allows easily swapping implementations
public class DbmsClient extends DbmsClientText {

	public DbmsClient(String ip, int port) {
		super(ip, port);
	}

}
