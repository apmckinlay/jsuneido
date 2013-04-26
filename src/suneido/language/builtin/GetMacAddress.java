/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import suneido.SuException;

public class GetMacAddress {

	public static String GetMacAddress() {
        try {
			return bytesToString(getMacAddress());
		} catch (UnknownHostException e) {
			throw new SuException(
					"GetMacAddress failed - UnknownHostException", e);
		} catch (SocketException e) {
			throw new SuException("GetMacAddress failed - SocketException", e);
		}
	}

	private static byte[] getMacAddress() throws UnknownHostException, SocketException {
		InetAddress address = InetAddress.getLocalHost();
		NetworkInterface ni = NetworkInterface.getByInetAddress(address);
		return ni.getHardwareAddress();
	}

	private static String bytesToString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++)
			sb.append((char) (bytes[i] & 0xff));
		return sb.toString();
	}

}
