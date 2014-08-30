/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.net.InetAddress;

import suneido.TheDbms;

public class ServerIP {

	public static String ServerIP() {
		InetAddress inetAddress = TheDbms.dbms().getInetAddress();
		return inetAddress == null ? "" : inetAddress.getHostAddress();
	}

}
