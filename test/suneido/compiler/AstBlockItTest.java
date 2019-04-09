/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.compiler.AstBlockIt;
import suneido.compiler.AstNode;
import suneido.compiler.Compiler;

public class AstBlockItTest {

	@Test
	public void test() {
		test(false, "");
		test(true, "it");
		test(true, "f(it)");
	}

	public static void test(boolean blockIt, String s) {
		AstNode ast = Compiler.parse("function () { " + s + "\n}");
		assertEquals(blockIt, AstBlockIt.check(ast.second()));
	}

}
