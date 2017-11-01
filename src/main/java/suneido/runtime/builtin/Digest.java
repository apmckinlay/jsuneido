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
	private static final BuiltinMethods methods = new BuiltinMethods(Digest.class);
	private final MessageDigest cksum;
	private final String className;

	private Digest(String which, String className) {
		try {
			cksum = MessageDigest.getInstance(which);
		} catch (NoSuchAlgorithmException e) {
			throw new SuException("can't access " + which, e);
		}
		this.className = className;
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

	@Override
	public String typeName() {
		return className;
	}

	public static class Clazz extends BuiltinClass {
		String which;
		String className;

		public Clazz(String which, String className) {
			super(className);
			this.which = which;
			this.className = className;
		}

		@Override
		protected Object newInstance(Object... args) {
			Args.massage(FunctionSpec.NO_PARAMS, args);
			return new Digest(which, className);
		}

		@Override
		public Object call(Object... args) {
			Digest digest = new Digest(which, className);
			if (args.length == 0)
				return digest;
			ArgsIterator iter = new ArgsIterator(args);
			while (iter.hasNext())
				Update(digest, iter.next());
			return Value(digest);
		}

		@Override
		public String toString() {
			return className + " /* builtin class */";
		}

	}

}
