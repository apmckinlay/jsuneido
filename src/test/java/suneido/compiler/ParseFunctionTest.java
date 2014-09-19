/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import org.junit.Test;

import suneido.compiler.AstGenerator;
import suneido.compiler.AstNode;
import suneido.compiler.Generator;
import suneido.compiler.Lexer;
import suneido.compiler.ParseFunction;

public class ParseFunctionTest {
	
	@Test
	public void if_comment() {
		parse("if 1 < 2\nPrint(12)");
		parse("if 1 < 2\n\nPrint(12)");
		parse("if 1 < 2\n\t\n\tPrint(12)");
		parse("if 1 < 2/**/\nPrint(12)");
		parse("if 1 < 2\n/**/Print(12)");
		parse("if 1 < 2\n/**/\nPrint(12)");
		parse("if 1 < 2//...\nPrint(12)");
		parse("if 1 < 2\n//...\nPrint(12)");
	}

	static void parse(String code) {
		Lexer lexer = new Lexer("function () {\n" + code + "\n}");
		AstGenerator generator = new AstGenerator();
		ParseFunction<AstNode, Generator<AstNode>> pc =
				new ParseFunction<AstNode, Generator<AstNode>>(lexer, generator);
		pc.parse();
	}
	
}
