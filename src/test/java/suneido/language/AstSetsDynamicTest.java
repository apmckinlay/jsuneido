/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AstSetsDynamicTest {

	@Test
	public void test() {
		test(false, "");
		test(false, "_x");
		test(false, "x = 5");
		test(false, "f = function () { _x = 5 }");
		test(false, "c = class { f() { _x = 5 } }");

		test(true, "_x = 5");
		test(true, "x = _x = 5");
		test(true, "++_x");
		test(true, "--_x");
		test(true, "_x++");
		test(true, "_x--");
		test(true, "_x += 5");
	}

	public static void test(boolean result, String s) {
		AstNode ast = Compiler.parse("function () { " + s + "\n}");
		assertEquals(result, AstSetsDynamic.check(ast));
	}

}
