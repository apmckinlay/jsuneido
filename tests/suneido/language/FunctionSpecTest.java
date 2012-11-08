/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static suneido.language.FunctionSpec.*;

public class FunctionSpecTest {

	@Test
	public void test_from() {
		assertSame(from(""), noParams);
		assertSame(from("string"), string);
		assertSame(from("value"), value);
		assertSame(from("value,value"), value2);
		assertSame(from("value, value"), value2);
		assertSame(from("block"), block);

		test("a,b,c", "params: a b c, defaults:");
		test("a, b, c", "params: a b c, defaults:");
		test("a,b,c=true", "params: a b c, defaults: true");
		test("a,b=123,c = hello", "params: a b c, defaults: 123 'hello'");
		test("a, b = 'hello world', c = false",
				"params: a b c, defaults: 'hello world' false");
	}

	private static void test(String params, String expected) {
		assertEquals("FunctionSpec(" + expected.replace('\'', '"') + ")",
				from(params).toString());
	}

}
