package suneido.language.builtin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import suneido.SuException;
import suneido.language.SuFunction1;
import suneido.language.Ops;
import suneido.util.Util;

public class Md5 extends SuFunction1 {

	@Override
	public Object call1(Object a) {
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
