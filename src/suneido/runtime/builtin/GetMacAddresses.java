/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import com.google.common.collect.Lists;

import suneido.SuException;
import suneido.SuObject;
import suneido.util.Util;

public class GetMacAddresses {
	private static final SuObject addrs = getMacAddresses();

	public static SuObject GetMacAddresses() {
		return addrs;
	}

	private static SuObject getMacAddresses() {
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
		return new SuObject(list).setReadonly();
	}
}
