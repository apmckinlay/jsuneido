/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static java.lang.Boolean.FALSE;
import static suneido.util.Util.array;

import java.util.zip.Adler32;
import java.util.zip.Checksum;

import suneido.language.*;

public class Adler32Class extends SuInstance {
	public static final SuClass singleton =
		new PrimitiveMethods("Adler32", Adler32Class.class) {
			@Override
			protected Object newInstance(Object... args) {
				Args.massage(FunctionSpec.noParams, args);
				return new Adler32Class();
			}
		};
	private final Checksum cksum = new Adler32();

	private Adler32Class() {
		super(singleton);
	}

	public static class CallClass extends SuMethod1 {
		{ params = new FunctionSpec(array("string"), FALSE); }

		@Override
		public Object eval1(Object self, Object a) {
			if (a == FALSE)
				return new Adler32Class();
			else {
				Checksum cksum = new Adler32();
				update(cksum, Ops.toStr(a));
				return (int) cksum.getValue();
			}
		}
	}

	public static class Update extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			update(((Adler32Class) self).cksum, Ops.toStr(a));
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
			return (int) (((Adler32Class) self).cksum.getValue());
		}
	}
}
