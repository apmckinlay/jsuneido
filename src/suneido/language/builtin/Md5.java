/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import suneido.SuException;
import suneido.language.Ops;
import suneido.language.Params;
import suneido.util.Util;

public class Md5 {

	@Params("string")
	public static String Md5(Object a) {
		String s = Ops.toStr(a);
		byte[] data = Util.stringToBytes(s);
		MessageDigest digest;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new SuException("Can't access MD5", e);
		}
		digest.update(data);
		return Util.bytesToString(digest.digest());
	}

}
