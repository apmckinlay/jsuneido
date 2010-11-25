/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static java.lang.Boolean.FALSE;
import static suneido.util.Util.array;

import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import suneido.language.*;

import com.google.common.collect.ImmutableMap;

public class Adler32Class extends SuInstance {
	public static final SuClass singleton = new SuClass("Adler32", null, methods()) {
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

	private static Map<String, SuMethod> methods() {
		ImmutableMap.Builder<String, SuMethod> b = ImmutableMap.builder();
		b.put("CallClass", new CallClass());
		b.put("Update", new Update());
		b.put("Value", new Value());
		return b.build();
	}

	private static class CallClass extends BuiltinMethod1 {
		{ params = new FunctionSpec(array("string"), FALSE); }

		@Override
		public Object eval0(Object self) {
			return eval1(self, FALSE);
		}
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

	private static class Update extends BuiltinMethod1 {
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

	private static class Value extends BuiltinMethod0 {
		@Override
		public Object eval0(Object self) {
			return (int) (((Adler32Class) self).cksum.getValue());
		}
	}
}
