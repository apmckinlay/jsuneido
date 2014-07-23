/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AstUsesThisTest {

	@Test
	public void test() {
		test(false, "");
		test(true, ".x");
		test(true, ".f()");
		test(true, "this.x");
		test(true, "this.f()");
		test(true, "f(this)");
		test(true, "return this");
		test(true, "super()");
		test(true, "super.f()");
		test(true, "b = { .a }");
	}

	public static void test(boolean usesThis, String s) {
		AstNode ast = Compiler.parse("function () { " + s + "\n}");
		assertEquals(usesThis, AstUsesThis.check(ast));
	}

}
