/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import suneido.SuContainer;
import suneido.SuException;
import suneido.util.Util;

import com.google.common.collect.Lists;

public class GetMacAddresses {

	public static SuContainer GetMacAddresses() {
		return new SuContainer(getMacAddresses());
	}

	private static ArrayList<String> getMacAddresses() {
		ArrayList<String> list = Lists.newArrayList();
		try {
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
					e.hasMoreElements(); ) {
				NetworkInterface ni = e.nextElement();
				if (ni.isLoopback() || ni.isVirtual() || ni.isPointToPoint())
					continue;
				byte[] mac = ni.getHardwareAddress();
				if (mac != null && mac.length > 0)
					list.add(Util.bytesToString(mac));
			}
		} catch (SocketException e) {
			throw new SuException("GetMacAddress failed - SocketException", e);
		}
		return list;
	}

}
