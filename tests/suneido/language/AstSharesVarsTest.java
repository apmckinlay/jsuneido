/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AstSharesVarsTest {

	@Test
	public void test_hasSharedVars() {
		test(false, "");
		test(false, "10.Times() { }");
		test(true, "10.Times() { ++p }");
		test(true, "x=1; b={ x=2 }");
		test(false, "x=1; b={ y=2 }");
		test(false, "x=1; b={|x| x=2 }");
		test(true, "x=1; b={|y| x=2 }");
	}

	public static void test(boolean hasShared, String s) {
		Lexer lexer = new Lexer("function (p) { " + s + "\n}");
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		assertEquals(hasShared, AstSharesVars.check(ast));
	}

}
