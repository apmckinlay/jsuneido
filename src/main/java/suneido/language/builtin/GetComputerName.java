/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.net.InetAddress;
import java.net.UnknownHostException;

import suneido.SuException;

public class GetComputerName {

	public static String GetComputerName() {
		try {
			return System.getProperty("os.name").contains("Windows")
					? System.getenv("COMPUTERNAME")
					: InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new SuException("GetComputerName", e);
		}
	}

}
