/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.zip.Checksum;

import suneido.SuValue;
import suneido.runtime.*;

public final class Adler32 extends SuValue {
	private final Checksum cksum = new java.util.zip.Adler32();
	private static final BuiltinMethods methods = new BuiltinMethods(Adler32.class);;

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	@Params("string")
	public static Object Update(Object self, Object a) {
		update(((Adler32) self).cksum, Ops.toStr(a));
		return self;
	}

	private static void update(Checksum cksum, String s) {
		for (int i = 0; i < s.length(); ++i)
			cksum.update(s.charAt(i));
	}

	public static Object Value(Object self) {
		return (int) (((Adler32) self).cksum.getValue());
	}

	public static final BuiltinClass clazz = new BuiltinClass() {

		@Override
		protected Object newInstance(Object... args) {
			Args.massage(FunctionSpec.NO_PARAMS, args);
			return new Adler32();
		}

		@Override
		public Object call(Object... args) {
			Adler32 adler32 = new Adler32();
			if (args.length == 0)
				return adler32;
			ArgsIterator iter = new ArgsIterator(args);
			while (iter.hasNext())
				Update(adler32, iter.next());
			return Value(adler32);
		}
	};

}
