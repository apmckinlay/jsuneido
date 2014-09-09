/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static suneido.runtime.FunctionSpec.*;

import org.junit.Test;

public class FunctionSpecTest {

	@Test
	public void test_from() {
		assertSame(from(""), NO_PARAMS);
		assertSame(from("string"), STRING);
		assertSame(from("value"), VALUE);
		assertSame(from("value,value"), VALUE2);
		assertSame(from("value, value"), VALUE2);
		assertSame(from("block"), BLOCK);

		test("a,b,c", "params: a b c, defaults:");
		test("a, b, c", "params: a b c, defaults:");
		test("a,b,c=true", "params: a b c, defaults: true");
		test("a,b=123,c = hello", "params: a b c, defaults: 123 'hello'");
		test("block, n = 1", "params: block n, defaults: 1");
		test("a, b = 'hello world', c = false",
				"params: a b c, defaults: 'hello world' false");
	}

	private static void test(String params, String expected) {
		assertEquals("FunctionSpec(" + expected.replace('\'', '"') + ")",
				from(params).toString());
	}

}
