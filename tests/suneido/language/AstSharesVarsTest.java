/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.util.Tr;

public class AstSharesVarsTest {

	@Test
	public void main() {
		test(false, "");
		test(false, "10.Times() { }");
		test(true, "10.Times() { ++p }");
		test(true, "x=1; b={ x=2 }");
		test(true, "b={ x=2 }; x");
		test(false, "x=1; b={ y=2 }");
		test(false, "x=1; b={|x| x=2 }");
		test(true, "x=1; b={|y| x=2 }");
		test(true, "b = { .a }");
		test(true, "b = { this }");
		test(true, "Do() { this }");
		test(false, "this; b = { }");
		test(true, "b = { super.f() }");
		test(true, "for (x in ob) Do() { x }");
		test(true, "try f() catch (e) Do() { e }");
		test(true, "x=1; b = {|y| ++x[y] }");
		test(true, "function (@args) { T() {|t| t.F(@args) } }");
	}

	public static void test(boolean hasShared, String code) {
		if (! code.startsWith("function"))
			code = "function (p) { " + code + "\n}";
		AstNode ast = Compiler.parse(code);
		assertEquals(hasShared, AstSharesVars.check(ast));
	}

	@Test
	public void test_closures() {
		ast("b = { x }",
			"(EQ (b) (BLOCK () ((x)) null))");
		ast("b = { p }",
			"(EQ (b) (BLOCK () ((p)) (CLOSURE)))");
		ast("b = { this }",
			"(EQ (b) (BLOCK () ((this)) (CLOSURE)))");
		ast("b = { .m }",
			"(EQ (b) (BLOCK () ((MEMBER=m (SELFREF))) (CLOSURE)))");
		ast("return { }",
			"(RETURN (BLOCK () ((NIL)) null))");
		ast("return { return { } }",
			"(RETURN (BLOCK () ((RETURN (BLOCK () ((NIL)) null))) null))");
		ast("return { return { p } }",
			"(RETURN (BLOCK () ((RETURN (BLOCK () ((p)) (CLOSURE)))) (CLOSURE)))");
		ast("F() {|f| p; Q() { f } }",
			"(CALL (F) ((ARG (STRING=block) (BLOCK ((f null)) ((p) " +
			"(CALL (Q) ((ARG (STRING=block) (BLOCK () ((f)) null))))) (CLOSURE)))))");
	}

	private void ast(String code, String expected) {
		AstNode ast = Compiler.parse("function (p) { " + code + "\n}");
		AstSharesVars.check(ast);
		ast = ast.second();
		String s = ast.toString();
		s = Tr.tr(s, " \n", " ").trim();
		s = s.replace("LIST ", "").replace("LIST", "").replace("IDENTIFIER=", "");
		assertEquals(expected, s.substring(1, s.length() - 1));
	}

}
