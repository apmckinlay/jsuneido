/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import suneido.SuException;
import suneido.util.Util;

public class GetMacAddress {

	public static String GetMacAddress() {
        try {
			return Util.bytesToString(getMacAddress());
		} catch (UnknownHostException e) {
			throw new SuException(
					"GetMacAddress failed - UnknownHostException", e);
		} catch (SocketException e) {
			throw new SuException("GetMacAddress failed - SocketException", e);
		}
	}

	private static byte[] getMacAddress() throws UnknownHostException, SocketException {
		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
				e.hasMoreElements(); ) {
			NetworkInterface ni = e.nextElement();
			if (ni.isLoopback() || ni.isVirtual() || ni.isPointToPoint())
				continue;
			byte[] mac = ni.getHardwareAddress();
			if (mac != null && mac.length > 0)
				return mac;
		}
		throw new SuException("GetMacAddress failed - no mac address found");
	}

}
