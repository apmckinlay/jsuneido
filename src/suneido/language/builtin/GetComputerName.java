/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.net.InetAddress;
import java.net.UnknownHostException;

import suneido.language.SuFunction0;

public class GetComputerName extends SuFunction0 {

	@Override
	public Object call0() {
		try {
			return System.getProperty("os.name").contains("Windows")
					? System.getenv("COMPUTERNAME")
					: InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException("GetComputerName", e);
		}
	}

	public static void main(String[] args) {
		System.out.println(new GetComputerName().call0());
	}

}
