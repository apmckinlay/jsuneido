/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.TheDbms;

public class ServerPort {

	public static Object ServerPort() {
		int port = TheDbms.serverPort();
		return port == 0 ? "" : port;
	}

}
