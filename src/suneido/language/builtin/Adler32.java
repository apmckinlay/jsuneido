/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static java.lang.Boolean.FALSE;
import static suneido.util.Util.array;

import java.util.zip.Checksum;

import suneido.SuValue;
import suneido.language.*;

public class Adler32 extends SuValue {
	private final Checksum cksum = new java.util.zip.Adler32();
	private static final BuiltinMethods methods = new BuiltinMethods(Adler32.class);

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static class Update extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			update(((Adler32) self).cksum, Ops.toStr(a));
			return self;
		}
	}

	private static void update(Checksum cksum, String s) {
		for (int i = 0; i < s.length(); ++i)
			cksum.update(s.charAt(i));
	}

	public static class Value extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return (int) (((Adler32) self).cksum.getValue());
		}
	}

	public static final BuiltinClass clazz = new BuiltinClass() {
		@Override
		protected Object newInstance(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
			return new Adler32();
		}
		FunctionSpec callFS = new FunctionSpec(array("string"), FALSE);
		@Override
		public Object call(Object... args) {
			args = Args.massage(callFS, args);
			if (args[0] == FALSE)
				return new Adler32();
			else {
				Checksum cksum = new java.util.zip.Adler32();
				update(cksum, Ops.toStr(args[0]));
				return (int) cksum.getValue();
			}
		}
	};

}
