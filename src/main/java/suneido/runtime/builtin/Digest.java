/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.*;
import suneido.util.Util;

public class Digest extends SuValue {
	private static final BuiltinMethods methods = new BuiltinMethods(Digest.class);;
	private final MessageDigest cksum;

	private Digest(String which) {
		try {
			cksum = MessageDigest.getInstance(which);
		} catch (NoSuchAlgorithmException e) {
			throw new SuException("can't access " + which, e);
		}
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	@Params("string")
	public static Object Update(Object self, Object a) {
		update(((Digest) self).cksum, Ops.toStr(a));
		return self;
	}

	private static void update(MessageDigest cksum, String s) {
		byte[] data = Util.stringToBytes(s);
		cksum.update(data);
	}

	public static Object Value(Object self) {
		return Util.bytesToString(((Digest) self).cksum.digest());

	}

	public static class Instance extends BuiltinClass {
		String which;

		public Instance(String which) {
			this.which = which;
		}

		@Override
		protected Object newInstance(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
			return new Digest(which);
		}

		@Override
		public Object call(Object... args) {
			Digest digest = new Digest(which);
			if (args.length == 0)
				return digest;
			ArgsIterator iter = new ArgsIterator(args);
			while (iter.hasNext())
				Update(digest, iter.next());
			return Value(digest);
		}
	}

}
