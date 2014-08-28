/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.junit.Assert.assertEquals;
import static suneido.util.testing.Throwing.assertThrew;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import suneido.SuException;
import suneido.Suneido;

public class CompileTest {

	/*
	 * a, b, c are parameters (so no null checks)
	 */
	@Test
	public void test_expressions() {
		test("return",
				"null, ARETURN");
		test("123",
				"123, ARETURN");
		test("0xffffffff",
				"-1, ARETURN");
		test("037777777777",
				"-1, ARETURN");
		test("0.1",
				"const0, ARETURN");
		test("true",
				"true, ARETURN");
		test("false",
				"false, ARETURN");
		test("return 123",
 				"123, ARETURN");
		test("b;;",
 				"b, POP");
		test("a",
				"a, ARETURN");
		test("return b",
				"b, ARETURN");
		test("x",
 				"x, null?, ARETURN");
		test("x;;",
 				"x, null?, POP");
		test("return x",
 				"x, null?, ARETURN");
		test("return a + b",
 				"a, b, add, ARETURN");
		test("-a",
				"a, uminus, ARETURN");
		test("! a",
				"a, not, ARETURN");
		test("a = b",
				"&a, b, DUP_X2, AASTORE, ARETURN");
		test("a = b",
 				"&a, b, DUP_X2, AASTORE, ARETURN");
		test("a = x;;",
 				"&a, x, null?, AASTORE");
		test("a *= b",
 				"&a, DUP2, AALOAD, b, mul, DUP_X2, AASTORE, ARETURN");
		test("a = b $ c",
 				"&a, b, c, cat, DUP_X2, AASTORE, ARETURN");
		test("a = b = c",
				"&a, &b, c, DUP_X2, AASTORE, DUP_X2, AASTORE, ARETURN");
		test("a = b = x",
				"&a, &b, x, null?, DUP_X2, AASTORE, DUP_X2, AASTORE, ARETURN");
		test("a = b; return c",
 				"&a, b, AASTORE, c, ARETURN");
		test("a.b = x",
			"a, 'b', x, null?, DUP_X2, putMem, ARETURN");
		test("a.b = x;;",
			"a, 'b', x, null?, putMem");
		test("a = b = c; return x",
				"&a, &b, c, DUP_X2, AASTORE, AASTORE, x, null?, ARETURN");
		test("-(a = b)",
				"&a, b, DUP_X2, AASTORE, uminus, ARETURN");
		test("123 is (a = b)",
				"123, &a, b, DUP_X2, AASTORE, is, ARETURN");
		test("(a = b) is 123",
				"&a, b, DUP_X2, AASTORE, 123, is, ARETURN");
		test("123 is (a = b)",
				"123, &a, b, DUP_X2, AASTORE, is, ARETURN");
		test("a[b = c]",
				"a, &b, c, DUP_X2, AASTORE, getMem, ARETURN");
		test("return this",
				"self, ARETURN");
		test("++a;;",
				"&a, DUP2, AALOAD, add1, AASTORE");
		test("++a",
				"&a, DUP2, AALOAD, add1, DUP_X2, AASTORE, ARETURN");
		test("--a;;",
				"&a, DUP2, AALOAD, sub1, AASTORE");
		test("a++",
				"&a, DUP2, AALOAD, DUP_X2, add1, AASTORE, ARETURN");
		test("a--;;",
				"&a, DUP2, AALOAD, sub1, AASTORE");
		test("a--",
				"&a, DUP2, AALOAD, DUP_X2, sub1, AASTORE, ARETURN");
		test("a.x",
				"a, 'x', getMem, ARETURN");
		test("a.x;;",
				"a, 'x', getMem, POP");
		test(".x",
				"self, 'x', getMem, ARETURN");
		test("a.x = b;;",
				"a, 'x', b, putMem");
		test("a.x = b",
				"a, 'x', b, DUP_X2, putMem, ARETURN");
		test("a[b]",
				"a, b, getMem, ARETURN");
		test("a[b];;",
				"a, b, getMem, POP");
		test("a[b] = c;;",
				"a, b, c, putMem");
		test("a[b] *= c;;",
				"a, b, DUP2, getMem, c, mul, putMem");
		test("a[b + 1]",
				"a, b, 1, add, getMem, ARETURN");
		test("a[b + 1];;",
				"a, b, 1, add, getMem, POP");
		test("a[++b];;",
				"a, &b, DUP2, AALOAD, add1, DUP_X2, AASTORE, getMem, POP");
		test("a[++b] = c;;",
				"a, &b, DUP2, AALOAD, add1, DUP_X2, AASTORE, c, putMem");
		test("G",
				"this, G, global, ARETURN");
		test("G@std",
				"this, G@std, global, ARETURN");
		test("return .a + .b",
				"self, 'a', getMem, self, 'b', getMem, add, ARETURN");
		test("123; 456; 789;",
				"789, ARETURN");
		test("a = #(1, a: 2);;",
				"&a, const0, AASTORE");
		test("#{1, a: 2}",
				"const0, ARETURN");
		test("return function () { }",
				"const0, ARETURN");
		test("a = function () { }",
				"&a, const0, DUP_X2, AASTORE, ARETURN");
		test("; { ; a ; } ;",
				"a, POP");
	}

	@Test
	public void conditional() {
		test("a ? b : c",
				"a, bool, IFFALSE L1, b, GOTO L2, L1, c, L2, ARETURN");
		test("a ? b : x;;",
				"a, bool, IFFALSE L1, b, GOTO L2, L1, x, L2, POP");
		test("a ? b : x",
				"a, bool, IFFALSE L1, b, GOTO L2, L1, x, L2, null?, ARETURN");
		test("a ? b() : c();;",
				"a, bool, IFFALSE L1, b, call, GOTO L2, L1, c, call, L2, POP");
		test("(a = b) ? (b = 123) : (c = 456)",
				"&a, b, DUP_X2, AASTORE, bool, IFFALSE L1, "
						+ "&b, 123, DUP_X2, AASTORE, GOTO L2, "
						+ "L1, &c, 456, DUP_X2, AASTORE, L2, ARETURN");
		test("a or b ? c : a",
				"a, bool, IFTRUE L1, b, bool, IFFALSE L2, " +
				"L1, c, GOTO L3, L2, a, L3, ARETURN");
		test("a and b ? c : a",
				"a, bool, IFFALSE L1, b, bool, IFFALSE L1, " +
				"c, GOTO L2, L1, a, L2, ARETURN");
		test("2 in (1, 2, 3)",
				"2, ASTORE 2, "
				+ "1, ALOAD 2, is_, IFTRUE L1, "
				+ "2, ALOAD 2, is_, IFTRUE L1, "
				+ "3, ALOAD 2, is_, IFTRUE L1, "
				+ "false, GOTO L2, L1, true, L2, ARETURN");
	}

	@Test
	public void calls() {
		test("a()",
				"a, call, ARETURN");
		test("a(b, c)",
				"a, b, c, call, ARETURN");
		test("a(x)",
				"a, x, null?, call, ARETURN");
		test("a(b = c, c)",
				"a, &b, c, DUP_X2, AASTORE, c, call, ARETURN");
		test("a(b, x: c)",
				"a, b, NAMED, 'x', c, call, ARETURN");
		test("a(b, x:)",
				"a, b, NAMED, 'x', true, call, ARETURN");
		test("a(b = c, c)",
				"a, &b, c, DUP_X2, AASTORE, c, call, ARETURN");
		test("G()",
 				"this, G, global call, ARETURN");
		test("a(@b)",
 				"a, EACH, b, call, ARETURN");
		test("a(@+1b)",
 				"a, EACH1, b, call, ARETURN");
		test("a = b();;",
 				"&a, b, call, null?, AASTORE");

		test("a.Size()",
				"a, 'Size', invoke0, ARETURN");
		test("(a = b).F()",
				"&a, b, DUP_X2, AASTORE, 'F', invoke0, ARETURN");
		test("return a.Size()",
				"a, 'Size', invoke0, ARETURN");
		test("a['Size']()",
				"a, 'Size', toMethodString, invoke0, ARETURN");
		test("a.Substr(b, c)",
				"a, 'Substr', b, c, invoke2, ARETURN");
		test(".f()",
				"self, 'f', invoke0, ARETURN");
		test("this.f()",
				"self, 'f', invoke0, ARETURN");
		test("this[a]()",
				"self, a, toMethodString, invoke0, ARETURN");
		test("a(123, x: 456)",
 				"a, 123, NAMED, 'x', 456, call, ARETURN");
		test("a(99: 'x')",
				"a, NAMED, 99, 'x', call, ARETURN");
		test("A().B()",
				"this, A, global call, 'B', invoke0, ARETURN");

		test("super.F()",
				"this, self, 'F', superInvoke, ARETURN");
	}

	@Test
	public void andor() {
		test("a or b",
				"a, bool, IFTRUE L1, b, bool, IFTRUE L1, " +
				"false, GOTO L2, L1, true, L2, ARETURN");
		test("a and b",
				"a, bool, IFFALSE L1, b, bool, IFFALSE L1, " +
				"true, GOTO L2, L1, false, L2, ARETURN");
		test("a and b and c",
				"a, bool, IFFALSE L1, " +
				"b, bool, IFFALSE L1, " +
				"c, bool, IFFALSE L1, " +
				"true, GOTO L2, L1, false, L2, ARETURN");
		test("true or b",
				"true, ARETURN");
		test("a or true",
				"true, ARETURN");
		test("false and b",
				"false, ARETURN");
		test("a and false",
				"false, ARETURN");
	}

	@Test
	public void test_new() {
		test("new c",
				"c, '<new>', invoke, ARETURN");
		test("new c(123)",
				"c, '<new>', 123, invoke, ARETURN");
		test("new G",
				"this, G, global, '<new>', invoke, ARETURN");
		test("new this",
				"self, '<new>', invoke, ARETURN");
		test("new this(a)",
				"self, '<new>', a, invoke, ARETURN");
	}
	@Test
	public void test_if() {
		test("if (a) b()",
				"a, bool, IFFALSE L1, b, call, POP, L1");
		test("if (a < b) c()",
				"a, b, lt_, IFFALSE L1, c, call, POP, L1");
		test("if (a = b) c()",
				"&a, b, DUP_X2, AASTORE, bool, IFFALSE L1, c, call, POP, L1");
		test("if (a) b() else c()",
				"a, bool, IFFALSE L1, b, call, POP, GOTO L2, L1, c, call, POP, L2");
		test("if (a && b) c else a",
				"a, bool, IFFALSE L1, b, bool, IFFALSE L1, " +
				"c, POP, GOTO L2, L1, a, POP, L2");
		test("if (a || b) c else a",
				"a, bool, IFTRUE L1, b, bool, IFFALSE L2, " +
				"L1, c, POP, GOTO L3, L2, a, POP, L3");
	}

	@Test
	public void test_loops() {
		test("do a() while (b)",
				"L1, a, call, POP, L2, b, bool, IFTRUE L1, L3");
		test("do a() while (b = c)",
				"L1, a, call, POP, L2, &b, c, DUP_X2, AASTORE, bool, IFTRUE L1, L3");

		test("while (a) b()",
				"GOTO L1, L2, b, call, POP, L1, a, bool, IFTRUE L2, L3");
		test("while (a = b) c()",
				"GOTO L1, L2, c, call, POP, L1, &a, b, DUP_X2, AASTORE, bool, IFTRUE L2, L3");
		test("while (a or b) c()",
				"GOTO L1, L2, c, call, POP, " +
				"L1, a, bool, IFTRUE L2, b, bool, IFTRUE L2, L3");
		test("while (a and b) c()",
				"GOTO L1, L2, c, call, POP, " +
				"L1, a, bool, IFFALSE L3, b, bool, IFTRUE L2, L3, L4");

		test("forever a()",
				"L1, a, call, POP, GOTO L1, L2");

		test("for(;;) a",
				"L1, a, POP, L2, GOTO L1, L3");

		test("for(b;;) a",
				"b, POP, L1, a, POP, L2, GOTO L1, L3");
		test("for(;b;) a",
				"GOTO L1, L2, a, POP, L3, L1, b, bool, IFTRUE L2, L4");
		test("for(;;b) a",
				"L1, a, POP, L2, b, POP, GOTO L1, L3");

		test("for(;b;c) a",
				"GOTO L1, L2, a, POP, L3, c, POP, L1, b, bool, IFTRUE L2, L4");

		test("for (a = 0; a < 4; ++a) b()",
				"&a, 0, AASTORE, GOTO L1, " +
				"L2, b, call, POP, " +
				"L3, &a, DUP2, AALOAD, add1, AASTORE, " +
				"L1, a, 4, lt_, IFTRUE L2, L4");

		test("for (a;b;c) break",
				"a, POP, GOTO L1, L2, GOTO L3, L4, c, POP, L1, b, bool, IFTRUE L2, L3");
		test("for (a;b;c) continue",
				"a, POP, GOTO L1, L2, GOTO L3, L3, c, POP, L1, b, bool, IFTRUE L2, L4");

		test("forever { a(); break; b() }",
				"L1, a, call, POP, GOTO L2, b, call, POP, GOTO L1, L2");
		test("forever { a(); continue; b() }",
				"L1, a, call, POP, GOTO L1, b, call, POP, GOTO L1, L2");
		test("do { a(); break; b() } while (c)",
				"L1, a, call, POP, GOTO L2, b, call, POP, L3, c, bool, IFTRUE L1, L2");
		test("do { a(); continue; b() } while (c)",
				"L1, a, call, POP, GOTO L2, b, call, POP, L2, c, bool, IFTRUE L1, L3");

		test("for (a in b) c()",
				"b, iterator, ASTORE 2, GOTO L1, " +
				"L2, ALOAD 2, next, args, SWAP, 0, SWAP, AASTORE, c, call, POP, " +
				"L1, ALOAD 2, hasNext, IFTRUE L2, L3");

		compile("for (a in b) try c() catch ;");
	}

	@Test
	public void test_switch() {
		test("switch (a) { }",
				"a, ASTORE 2, 'unhandled switch case', throw, L1, L2");
		test("switch (a) { default: b() }",
				"a, ASTORE 2, b, call, POP, L1, L2");
		test("switch (a) { case 123: b() }",
				"a, ASTORE 2, 123, ALOAD 2, is_, IFFALSE L1, L2, b, call, POP, GOTO L3, L1, "
				+ "'unhandled switch case', throw, L4, L3");
		test("switch (a = b) { case 123: c() }",
				"&a, b, DUP_X2, AASTORE, ASTORE 2, 123, ALOAD 2, is_, IFFALSE L1, L2, "
				+ "c, call, POP, GOTO L3, L1, 'unhandled switch case', throw, L4, L3");
		test("switch (a) { case 123,456: b() }",
				"a, ASTORE 2, 123, ALOAD 2, is_, IFTRUE L1, 456, ALOAD 2, is_, IFFALSE L2, L1, "
				+ "b, call, POP, GOTO L3, L2, 'unhandled switch case', throw, L4, L3");
	}

	@Test
	public void test_exceptions() {
		test("throw 'oops'",
				"'oops', throw");
		test("throw a",
				"a, throw");
		test("throw a = b",
				"&a, b, DUP_X2, AASTORE, throw");
		test_e("try a()",
				"try L0 L1 L2, L3, L0, a, call, POP, L1, GOTO L4, L2, POP, L4");
		test_e("try a() catch ;",
				"try L0 L1 L2, L3, L0, a, call, POP, L1, GOTO L4, L2, POP, L4");
		test_e("try a() catch b()",
				"try L0 L1 L2, L3, L0, a, call, POP, L1, GOTO L4, L2, POP, b, call, POP, L4");
		test_e("try a() catch(e) b()",
				"try L0 L1 L2, L3, L0, a, call, POP, L1, GOTO L4, L2, " +
					"catchMatch, args, SWAP, 4, SWAP, AASTORE, b, call, POP, L4");
		test_e("try a() catch(e, 'x') b()",
				"try L0 L1 L2, L3, L0, a, call, POP, L1, GOTO L4, L2, " +
					"'x', catchMatch, args, SWAP, 4, SWAP, AASTORE, b, call, POP, L4");
	}

	@Test
	public void test_block() {
		test_b0("Do() { this }",
				"self, ARETURN");
		test_b0("Do() { .a + .b }",
				"self, 'a', getMem, self, 'b', getMem, add, ARETURN");
		test_b0("Do() { return 123 }",
				"123, blockReturn");

		test_e("return { }",
				"try L0 L1 L2, L3, const0, L0, ARETURN, " +
				"L1, L2, blockReturnHandler, ARETURN");
		test_e("return { this }",
				"try L0 L1 L2, L3, block, L0, ARETURN, " +
				"L1, L2, blockReturnHandler, ARETURN");
		test_e("b = { }",
				"try L0 L1 L2, L3, &b, const0, L0, DUP_X2, AASTORE, ARETURN, " +
				"L1, L2, blockReturnHandler, ARETURN");
		test_e("b = { a }",
				"try L0 L1 L2, L3, &b, block, L0, DUP_X2, AASTORE, ARETURN, " +
				"L1, L2, blockReturnHandler, ARETURN");
		compile("Foreach(a, { })");
		compile("Foreach(a) { }");
		compile("Plugins().Foreach(a, { })");
		compile("Plugins.Foreach(a) { }");
		compile("Plugins().Foreach(a) { }");
		compile("b = { .001 }");
		test_e("b = { return 123 }",
				"try L0 L1 L2, L3, &b, const0, L0, DUP_X2, AASTORE, ARETURN, " +
				"L1, L2, blockReturnHandler, ARETURN");
		test_e("b = { a; return 123 }",
				"try L0 L1 L2, L3, &b, block, L0, DUP_X2, AASTORE, ARETURN, " +
				"L1, L2, blockReturnHandler, ARETURN");
	}

	@Test
	public void test_block_break() {
		compile("b = { break }");
		compile("Foreach(a, { break })");
		compile("Foreach(a) { break }");
		compile("Plugins().Foreach(a, { break })");
		compile("Plugins().Foreach(a) { break }");
	}

	@Test
	public void test_optimize_args() {
		test("x(11, a, 22, a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11)",
			"x, 11, a, EACH, const0, call, ARETURN");
		test("x('', y(a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11))",
			"x, '', y, EACH, const0, call, null?, call, ARETURN");
		test("x(a: 1, b: 2, c: 3, d: 4, e: 5, V: a, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11)",
			"x, NAMED, 'V', a, EACH, const0, call, ARETURN");
		test("A(a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11)",
			"this, A, EACH, const0, global call, ARETURN");
		test("[a: 1, b: 2, c: 3, d: 4, e: 5, V: a, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11]",
				"NAMED, 'V', a, EACH, const0, Record, ARETURN");
	}

	@Test
	public void test_super() {
		assertThrew(() -> {
			test("a = super.y", "");
		}, SuException.class, "syntax error at line 1.*: invalid use of super"); 

		test("super.Fn()", "this, self, 'Fn', superInvoke, ARETURN");
	}

	@Test
	public void test_compile_builtin_calls() {
		test("Object()",
			"Object, ARETURN");
		test("Object(a, b)",
			"a, b, Object, ARETURN");
		test("Object(a: b)",
			"NAMED, 'a', b, Object, ARETURN");
	}

	private static void test(String expr, String expected) {
		test(expr, expected,
				")Ljava/lang/Object;\n   L0\n");
	}
	private static void test_e(String expr, String expected) {
		test(expr, expected,
				")Ljava/lang/Object;\n");
	}
	private static void test_b0(String expr, String expected) {
		String s = compile(expr);
		s = simplify(s,
				")Ljava/lang/Object;\n   L0");
		s = before(s, ", INVOKESPECIAL suneido/language/BlockReturnException.<init>");
		assertEquals(expr, expected, s);
	}
	private static void test(String expr, String expected, String after) {
		String s = compile(expr);
		s = after(s, "class suneido/language/Test ");
		s = simplify(s, after);
		assertEquals(expr, expected, s);
	}

	private static String compile(String s) {
		//System.out.println("====== " + s);
		if (!s.startsWith("class") && !s.startsWith("function")
				&& !s.startsWith("#("))
			s = "function (a,b,c,d,e) { " + s + " }";
		StringWriter sw = new StringWriter();
		Compiler.compile("Test", s, new PrintWriter(sw), true);
		return after(sw.toString(), "\n\n");
	}

	private static String simplify(String r, String after) {
		//System.out.println(r);
		int SELF = 9;
		int ARGS = 9;
		if (r.contains("public eval(")) {
			SELF = 1;
			ARGS = 2;
		} else if (r.contains("public eval")) {
			SELF = 1;
		} else if (r.contains("public call(")) {
			ARGS = 1;
		} else if (r.contains("public call3")) {
		} else
			assert false : "unknown definition type";
		r = after(r, after);
		r = before(r, "    LOCALVARIABLE");
		//System.out.println(r);
		r = r.substring(0, r.length() - 6); // label
		r = r.trim();
		r = r.replace("\n", ", ");
		r = r.replace('"', '\'');
		r = r.replaceAll(" +", " ");
		String[][] simplify = {
			{ "Ljava/lang/", "" },
			{ "ALOAD 0", "this" },
			{ "ALOAD " + SELF, "self" },
			{ "ALOAD " + ARGS, "args" },
			{ "this, GETFIELD suneido/language/SuCallable.params : Lsuneido/language/FunctionSpec;, ", "" },
			{ "this, args, INVOKESPECIAL suneido/language/SuCallable.massage ([Object;)[Object;, ASTORE 1, ", "" },
			{ "this, args, INVOKESPECIAL suneido/language/SuCallable.massage ([Object;)[Object;, ASTORE 2, ", "" },
			{ "args, ICONST_0, AALOAD", "a" },
			{ "args, ICONST_1, AALOAD", "b" },
			{ "args, ICONST_2, AALOAD", "c" },
			{ "args, ICONST_5, AALOAD", "x" },
			{ "args, BIPUSH 6, AALOAD", "y" },
			{ "args, ICONST_0", "&a" },
			{ "args, ICONST_1", "&b" },
			{ "args, ICONST_2", "&c" },
			{ "ICONST_M1", "-1" },
			{ "ICONST_", "" },
			{ ", ACONST_NULL, ARETURN", "" },
			{ "ACONST_NULL", "null" },
			{ "ANEWARRAY Object", "new Object[]" },
			{ "GETSTATIC suneido/language/SuClass.", "" },
			{ " : LString;", "" },
			{ "GETSTATIC suneido/language/Args$Special.", "" },
			{ " : Lsuneido/language/Args$Special;", "" },
			{ "INVOKESPECIAL suneido/language/SuCallable.contextGet", "global" },
			{ "INVOKESPECIAL suneido/language/SuCallable.invoke", "global call" },
			{ "INVOKESTATIC suneido/language/Ops.", "" },
			{ "get (Object;Object;)Object;", "getMem" },
			{ "put (Object;Object;Object;)V", "putMem" },
			{ " (Object;)Z", "" },
			{ " (Object;)Object;", "" },
			{ " (Object;)Number;", "" },
			{ " (Object;)String;", "" },
			{ " (Object;)Boolean;", "" },
			{ " (Object;Object;)Z", "" },
			{ " (Object;Object;)Number;", "" },
			{ " (Object;Object;)String;", "" },
			{ " (Object;Object;)Boolean;", "" },
			{ " (Object;Object;)Object;", "" },
			{ " (Object;Object;Object;)Object;", "" },
			{ " (Object;String;)Object;", "" },
			{ " (Object;String;Object;)Object;", "" },
			{ " (Object;String;Object;Object;)Object;", "" },
			{ " (Object;Object;Object;Object;)Object;", "" },
			{ " (Object;Object;Object;Object;Object;)Object;", "" },
			{ " (Object;Object;Object;Object;Object;Object;)Object;", "" },
			{ " (Object;Object;Object;Object;Object;Object;Object;)Object;", "" },
			{ " (Object;[Object;)Object;", "" },
			{ " (Object;String;[Object;)Object;", "" },
			{ " (I)Object;", "" },
			{ " (I[Object;)Object;", "" },
			{ "DUP, IFNONNULL L1, throwNoReturnValue ()V, L1", "null?" },
			{ "DUP, IFNONNULL L1, throwUninitializedVariable ()V, L1", "null?" },
			{ "DUP, IFNONNULL L2, throwUninitializedVariable ()V, L2", "null?" },
			{ "DUP, IFNONNULL L3, throwUninitializedVariable ()V, L3", "null?" },
			{ "DUP, IFNONNULL L3, throwNoValue ()V, L3", "null?" },
			{ "LDC 'Test', INVOKESTATIC suneido/language/Constants.get (LString;)[LSuValue;, DUP, ASTORE 2", "const" },
			{ "GETSTATIC suneido/language/Test.const0 : Object;", "const0" },
			{ "toIntBool (Object;)I", "bool" },
			{ "IFEQ", "IFFALSE" },
			{ "IFNE", "IFTRUE" },
			{ "NEW suneido/language/SuBlock, DUP, const0, self, args, INVOKESPECIAL suneido/language/SuBlock.<init> (Object;Object;[Object;)V", "block" },
			{ "NEW suneido/language/SuBlock0, DUP, const0, self, args, INVOKESPECIAL suneido/language/SuBlock0.<init> (Object;Object;[Object;)V", "block" },
			{ "NEW suneido/language/SuBlock0, DUP, const0, null, args, INVOKESPECIAL suneido/language/SuBlock0.<init> (Object;Object;[Object;)V", "block" },
			{ " INVOKESTATIC java/lang/Integer.valueOf (I)Integer;,", "" },
			{ "BIPUSH ", "" },
			{ "SIPUSH ", "" },
			{ "LDC ", "" },
			{ "exception (Object;)Throwable;, ATHROW", "throw" },
			{ "TRYCATCHBLOCK L0 L1 L2 java/lang/Throwable", "try L0 L1 L2" },
			{ "TRYCATCHBLOCK L0 L1 L2 suneido/language/BlockReturnException", "try L0 L1 L2" },
			{ "catchMatch (Throwable;)Lsuneido/language/Except;", "catchMatch" },
			{ "catchMatch (Throwable;String;)Lsuneido/language/Except;", "catchMatch" },
			{ "INVOKEVIRTUAL suneido/SuException.toString ()String;", "toString" },
			{ "GETFIELD suneido/language/BlockReturnException.returnValue : Object;", ".returnValue" },
			{ "GETFIELD suneido/language/BlockReturnException.locals : [Object;", ".locals" },
			{ "INVOKESPECIAL suneido/language/SuCallable.superInvoke", "superInvoke" },
			{ "GETSTATIC java/lang/Boolean.TRUE : Boolean;", "true" },
			{ "GETSTATIC java/lang/Boolean.FALSE : Boolean;", "false" },
			{ "INVOKESTATIC suneido/language/ArgArray.buildN ()[Object;, ", "" },
			{ "INVOKESTATIC suneido/language/ArgArray.buildN (Object;Object;)[Object;, ", "" },
			{ "INVOKESTATIC suneido/language/builtin/ObjectClass.create ([Object;)Object;", "Object" },
			{ "INVOKESTATIC suneido/language/builtin/RecordClass.create ([Object;)Object;", "Record" },
			{ "blockReturnException (Object;I)Lsuneido/language/BlockReturnException;, ATHROW", "blockReturn" },
			{ "blockReturnHandler (Lsuneido/language/BlockReturnException;I)Object;", "blockReturnHandler" },
			{ "0, ANEWARRAY java/lang/Object, ", "" },
			{ "1, ANEWARRAY java/lang/Object, ", "" },
			{ "2, ANEWARRAY java/lang/Object, ", "" },
			{ "3, ANEWARRAY java/lang/Object, ", "" },
			{ "4, ANEWARRAY java/lang/Object, ", "" },
			{ "5, ANEWARRAY java/lang/Object, ", "" },
			{ "6, ANEWARRAY java/lang/Object, ", "" },
			{ "DUP, 0, ", "" },
			{ "AASTORE, DUP, 1, ", "" },
			{ "AASTORE, DUP, 2, ", "" },
			{ "AASTORE, DUP, 3, ", "" },
			{ "AASTORE, DUP, 4, ", "" },
			{ "AASTORE, DUP, 5, ", "" },
			{ "AASTORE, call", "call" },
			{ "AASTORE, global call", "global call" },
			{ "AASTORE, invoke", "invoke" },
			{ "AASTORE, Object", "Object" },
			{ "AASTORE, Record", "Record" },
			{ "call0", "call" },
			{ "call1", "call" },
			{ "call2", "call" },
			{ "call3", "call" },
			{ "call4", "call" },
		};
		for (String[] simp : simplify)
			r = r.replace(simp[0], simp[1]);
		r = r.replaceAll("[0-9]+, blockReturn", "blockReturn");
		r = globalSlotToName(r);
		return r;
	}

	private static String globalSlotToName(String r) {
		Pattern p = Pattern.compile("this, ([0-9]+), (EACH, const0, )?global");
		Matcher m = p.matcher(r);
		if (! m.find())
			return r;
		int i = Integer.valueOf(m.group(1));
		String name = Suneido.context.nameForSlot(i);
		return r.substring(0, m.start(1)) + name + r.substring(m.end(1));
	}

	private static String after(String r, String s) {
		int i = r.indexOf(s);
		return r.substring(i + s.length(), r.length());
	}

	private static String before(String r, String s) {
		int i = r.indexOf(s);
		if (i == -1)
			return r;
		return r.substring(0, i);
	}

}
