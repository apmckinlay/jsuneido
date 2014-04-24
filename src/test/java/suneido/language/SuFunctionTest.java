/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SuFunctionTest {

	@Test
	public void test() {
		Object f = new MyFunc();
		String s = "fred";
		assertEquals(s, Ops.call(f, s));
	}

	static class MyFunc extends SuFunction {
		static final FunctionSpec params =
				new FunctionSpec(new String[] { "value" }, 1);

		@Override
		public Object call(Object... args) {
			Args.massage(params, args);
			return args[0];
		}
	}
}
