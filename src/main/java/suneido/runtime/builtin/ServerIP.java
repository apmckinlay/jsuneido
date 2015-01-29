/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.TheDbms;

public class ServerIP {

	public static String ServerIP() {
		String ip = TheDbms.serverIP();
		return ip == null ? "" : ip;
	}

}
