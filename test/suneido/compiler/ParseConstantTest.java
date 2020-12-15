/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import suneido.SuException;
import suneido.runtime.Ops;

public class ParseConstantTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@After
	public void restoreQuoting() {
		Ops.default_single_quotes = false;
	}

	@Test
	public void constants() {
		constant("true",
			"(VALUE=true)");
		constant("false",
			"(VALUE=false)");
		constant("123",
			"(VALUE=123)");
		constant("+123",
			"(VALUE=123)");
		constant("-123",
			"(VALUE=-123)");
		constant("123.",
			"(VALUE=123)");
		constant("'xyz'",
			"(VALUE='xyz')");
		constant("#abc",
			"(VALUE='abc')");
		constant("#()",
			"(OBJECT=#())");
		constant("#{}",
			"(OBJECT=[])");
		constant("#(123)",
			"(OBJECT=#(123))");
		constant("#{name: 'fred'}",
			"(OBJECT=[name: 'fred'])");
		constant("#(1, 'x', a: #y, b: true)",
			"(OBJECT=#(1, 'x', a: 'y', b:))");
		constant("#20090219",
			"(VALUE=#20090219)");
		constant("#foo",
			"(VALUE='foo')");
		constant("Global",
			"(VALUE='Global')");
		constant("function () { }",
			"(FUNCTION (LIST) (LIST (NIL)))");
		constant("function\n () { }",
			"(FUNCTION (LIST) (LIST (NIL)))");
		constant("function ()\n { }",
			"(FUNCTION (LIST) (LIST (NIL)))");
		constant("function (@args) { }",
			"(FUNCTION (LIST (IDENTIFIER=@args null)) (LIST (NIL)))");
		constant("function (a, b, c = 1, d = 2) { }",
			"(FUNCTION (LIST (IDENTIFIER=a null) (IDENTIFIER=b null) (IDENTIFIER=c (VALUE=1)) (IDENTIFIER=d (VALUE=2))) (LIST (NIL)))");
		constant("class { }",
			"(CLASS=Class# null (VALUE={}))");
		constant("class\n { }",
			"(CLASS=Class# null (VALUE={}))");
		constant("class : Base { }",
			"(CLASS=Class# (VALUE='Base') (VALUE={}))");
		constant("Base { }",
			"(CLASS=Class# (VALUE='Base') (VALUE={}))");
		constant("Base { a: }",
			"(CLASS=Class# (VALUE='Base') (VALUE={Class#_a=true}))");
		constant("class { f() { x } }",
			"(CLASS=Class# null (VALUE={Class#_f=(METHOD (LIST) (LIST (IDENTIFIER=x)))}))");
		constant("#({})",
			"(OBJECT=#((OBJECT=[])))");
		constant("#([])",
			"(OBJECT=#((OBJECT=[])))");
		constant("#([a:])",
			"(OBJECT=#((OBJECT=[a:])))");
		constant("#(class: 123)",
			"(OBJECT=#(class: 123))");
		constant("#(function(){})",
			"(OBJECT=#((FUNCTION (LIST) (LIST (NIL)))))");
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
				new ParseConstant<>(lexer, generator);
		AstNode ast = pc.parse(null);
		String actual = ast.toString().replace("\n", " ").replaceAll(" +", " ");
		actual = actual.replaceAll("Class[0-9]+", "Class#");
//System.out.println("\t\t\"" + actual.substring(23, actual.length() - 2) + "\");");
		assertEquals(expected, actual);
	}

}
