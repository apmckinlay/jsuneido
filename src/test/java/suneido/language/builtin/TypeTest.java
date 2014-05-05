/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static org.junit.Assert.assertEquals;
import static suneido.language.Compiler.eval;

import org.junit.Test;

public class TypeTest {
	@Test
	public void test() {
		test("Type(123)", "Number");
		test("Type('sss')", "String");
		test("Type(1.2)", "Number");
		test("Type(#())", "Object");
		test("Type(#{})", "Record");
		test("shared = 1; Type({|x| shared })", "Block");
		test("Type(class { })", "Class");
		test("Type(new class { })", "Object");
		test("Type(function () { })", "Function");
		test("Type(Buffer(10, ''))", "Buffer");
	}

	public static void test(String expr, String result) {
		assertEquals(result, eval(expr));
	}
}
