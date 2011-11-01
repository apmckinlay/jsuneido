/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AstBlockItTest {

	@Test
	public void test() {
		test(false, "");
		test(true, "it");
		test(true, "f(it)");
	}

	public static void test(boolean blockIt, String s) {
		Lexer lexer = new Lexer("function () { " + s + "\n}");
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		assertEquals(blockIt, AstBlockIt.check(ast.second()));
	}

}
