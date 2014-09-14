/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.runtime.Args;
import suneido.runtime.FunctionSpec;
import suneido.runtime.Ops;
import suneido.runtime.SuCallBase;

public class SuCallBaseTest {

	@Test
	public void test() {
		Object f = new MyFunc();
		String s = "fred";
		assertEquals(s, Ops.call(f, s));
	}

	static class MyFunc extends SuCallBase {
		static final FunctionSpec params =
				new FunctionSpec(new String[] { "value" }, 1);

		@Override
		public Object call(Object... args) {
			Args.massage(params, args);
			return args[0];
		}
	}
}
