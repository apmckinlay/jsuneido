/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuException;
import suneido.compiler.AstGenerator;
import suneido.compiler.AstNode;
import suneido.compiler.Generator;
import suneido.compiler.Lexer;
import suneido.compiler.ParseConstant;

public class ParseConstantTest {
	@Test
	public void constants() {
		constant("true",
			"(TRUE)");
		constant("false",
			"(FALSE)");
		constant("123",
			"(NUMBER=123)");
		constant("'xyz'",
			"(STRING=xyz)");
		constant("#abc",
			"(SYMBOL=abc)");
		constant("#()",
			"(OBJECT)");
		constant("#{}",
			"(RECORD)");
		constant("#(123)",
			"(OBJECT (MEMBER null (NUMBER=123)))");
		constant("#{name: fred}",
			"(RECORD (MEMBER (STRING=name) (STRING=fred)))");
		constant("#(1, 'x', a: #y, b: true)",
			"(OBJECT (MEMBER null (NUMBER=1)) (MEMBER null (STRING=x)) (MEMBER (STRING=a) (SYMBOL=y)) (MEMBER (STRING=b) (TRUE)))");
		constant("+123",
			"(NUMBER=123)");
		constant("-123",
			"(NUMBER=-123)");
		constant("#20090219",
			"(DATE=20090219)");
		constant("#foo",
			"(SYMBOL=foo)");
		constant("#'foo bar'",
			"(SYMBOL=foo bar)");
		constant("Global",
			"(STRING=Global)");
		constant("function () { }",
			"(FUNCTION (LIST) (LIST (NIL)))");
		constant("function\n () { }",
			"(FUNCTION (LIST) (LIST (NIL)))");
		constant("function ()\n { }",
			"(FUNCTION (LIST) (LIST (NIL)))");
		constant("function (@args) { }",
			"(FUNCTION (LIST (IDENTIFIER=@args null)) (LIST (NIL)))");
		constant("function (a, b, c = 1, d = 2) { }",
			"(FUNCTION (LIST (IDENTIFIER=a null) (IDENTIFIER=b null) (IDENTIFIER=c (NUMBER=1)) (IDENTIFIER=d (NUMBER=2))) (LIST (NIL)))");
		constant("class { }",
			"(CLASS null (LIST))");
		constant("class\n { }",
			"(CLASS null (LIST))");
		constant("class : Base { }",
			"(CLASS (STRING=Base) (LIST))");
		constant("Base { }",
			"(CLASS (STRING=Base) (LIST))");
		constant("Base { a: }",
			"(CLASS (STRING=Base) (LIST (MEMBER (STRING=a) (TRUE))))");
		constant("Base { 12: 34 }",
			"(CLASS (STRING=Base) (LIST (MEMBER (NUMBER=12) (NUMBER=34))))");
		constant("Base { -12: 'abc' }",
			"(CLASS (STRING=Base) (LIST (MEMBER (NUMBER=-12) (STRING=abc))))");
		constant("class { a: 1; b: 2, c: 3 \n d: 4}",
			"(CLASS null (LIST (MEMBER (STRING=a) (NUMBER=1)) (MEMBER (STRING=b) (NUMBER=2)) (MEMBER (STRING=c) (NUMBER=3)) (MEMBER (STRING=d) (NUMBER=4))))");
		constant("class { f() { x } }",
			"(CLASS null (LIST (MEMBER (STRING=f) (METHOD (LIST) (LIST (IDENTIFIER=x))))))");
		constant("#()",
			"(OBJECT)");
		constant("#{}",
			"(RECORD)");
		constant("#(1, 'a', b: 2)",
			"(OBJECT (MEMBER null (NUMBER=1)) (MEMBER null (STRING=a)) (MEMBER (STRING=b) (NUMBER=2)))");
		constant("#({})",
			"(OBJECT (MEMBER null (RECORD)))");
		constant("#(class: 123)",
			"(OBJECT (MEMBER (STRING=class) (NUMBER=123)))");
	}

	@Test(expected = SuException.class)
	public void unnamed_members() {
		constant("class { one\n two }",
			"(CLASS null (LIST (MEMBER null (STRING=one)) (MEMBER null (STRING=two))))");
	}

	static void constant(String code, String expected) {
//System.out.println("\tcode(\"" + code.replace("\n", "\\n") + "\",");
				Lexer lexer = new Lexer(code);
				AstGenerator generator = new AstGenerator();
				ParseConstant<AstNode, Generator<AstNode>> pc =
						new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
				AstNode ast = pc.parse();
				String actual = ast.toString().replace("\n", " ").replaceAll(" +", " ");
//System.out.println("\t\t\"" + actual.substring(23, actual.length() - 2) + "\");");
				assertEquals(expected, actual);
	}

}
