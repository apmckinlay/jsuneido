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
	}

	public static void test(boolean usesThis, String s) {
		Lexer lexer = new Lexer("function () { " + s + "\n}");
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		assertEquals(usesThis, AstUsesThis.check(ast));
	}

}
