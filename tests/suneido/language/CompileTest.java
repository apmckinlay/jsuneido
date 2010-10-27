package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static suneido.language.Ops.display;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

import suneido.SuException;

public class CompileTest {
	//@Test
	public void blocks() {
		test("return { }",
				"block, ARETURN");
	}

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
				"0=.1, ARETURN");
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
//				"&a, &b, x, DUP_X2, AASTORE, DUP_X2, AASTORE, null?, ARETURN");
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
//				"&a, DUP2, AALOAD, DUP_X2, sub1, AASTORE, POP");
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
				"'G', global, ARETURN");
		test("return .a + .b",
				"self, 'a', getMem, self, 'b', getMem, add, ARETURN");
		test("123; 456; 789;",
				"789, ARETURN");
		test("a = #(1, a: 2);;",
				"&a, 0=#(1, a: 2), AASTORE");
		test("#{1, a: 2}",
				"0=[1, a: 2], ARETURN");
		test("return function () { }",
				"0=Test$f, ARETURN");
		test("a = function () { }",
				"&a, 0=Test$f, DUP_X2, AASTORE, ARETURN");
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
				"a, bool, IFFALSE L1, b, callN, GOTO L2, L1, c, callN, L2, POP");
		test("(a = b) ? (b = 123) : (c = 456)",
				"&a, b, DUP_X2, AASTORE, bool, IFFALSE L1, "
						+ "&b, 123, DUP_X2, AASTORE, GOTO L2, "
						+ "L1, &c, 456, DUP_X2, AASTORE, L2, ARETURN");
	}

	@Test
	public void calls() {
		test("a()",
				"a, callN, ARETURN");
		test("a(b, c)",
				"a, b, c, callN, ARETURN");
		test("a(x)",
				"a, x, null?, callN, ARETURN");
		test("a(b = c)",
				"a, &b, c, DUP_X2, AASTORE, callN, ARETURN");
		test("a(b, x: c)",
				"a, b, NAMED, 'x', c, callN, ARETURN");
		test("a(b, x:)",
				"a, b, NAMED, 'x', true, callN, ARETURN");
		test("a(b = c)",
				"a, &b, c, DUP_X2, AASTORE, callN, ARETURN");
		test("G()",
 				"'G', global, callN, ARETURN");
		test("a(@b)",
 				"a, EACH, b, callN, ARETURN");
		test("a(@+1b)",
 				"a, EACH1, b, callN, ARETURN");
		test("a = b();;",
 				"&a, b, callN, null?, AASTORE");

		test("a.Size()",
				"a, 'Size', invokeN, ARETURN");
		test("(a = b).F()",
				"&a, b, DUP_X2, AASTORE, 'F', invokeN, ARETURN");
		test("return a.Size()",
				"a, 'Size', invokeN, ARETURN");
		test("a['Size']()",
				"a, 'Size', toMethodString, invokeN, ARETURN");
		test("a.Substr(b, c)",
				"a, 'Substr', b, c, invokeN, ARETURN");
		test(".f()",
				"self, 'f', invokeN, ARETURN");
		test("this.f()",
				"self, 'f', invokeN, ARETURN");
		test("this[a]()",
				"self, a, toMethodString, invokeN, ARETURN");
		test("a(123, x: 456)",
 				"a, 123, NAMED, 'x', 456, callN, ARETURN");
		test("a(99: 'x')",
				"a, NAMED, 99, 'x', callN, ARETURN");
		test("A().B()",
				"'A', global, callN, 'B', invokeN, ARETURN");
		test("A(a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11)",
				"'A', global, EACH, 0=#(f: 6, g: 7, d: 4, e: 5, b: 2, c: 3, "
						+ "a: 1, j: 10, k: 11, h: 8, i: 9), callN, ARETURN");

		test("Object()",
				"Object, ARETURN");
		test("Object(a, b)",
				"a, b, Object, ARETURN");

		test("super.F()",
				"this, self, 'F', superInvokeN, ARETURN");
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
	}
	@Test public void test_new() {
		test("new c",
				"c, '<new>', invokeN, ARETURN");
		test("new c(123)",
				"c, '<new>', 123, invokeN, ARETURN");
		test("new G",
				"'G', global, '<new>', invokeN, ARETURN");
		test("new this",
				"self, '<new>', invokeN, ARETURN");
		test("new this(a)",
				"self, '<new>', a, invokeN, ARETURN");
	}
	@Test public void test_if() {
		test("if (a) b()",
				"a, bool, IFFALSE L1, b, callN, POP, L1");
		test("if (a < b) c()",
				"a, b, lt_, IFFALSE L1, c, callN, POP, L1");
		test("if (a = b) c()",
				"&a, b, DUP_X2, AASTORE, bool, IFFALSE L1, c, callN, POP, L1");
		test("if (a) b() else c()",
				"a, bool, IFFALSE L1, b, callN, POP, GOTO L2, L1, c, callN, POP, L2");
	}
	@Test public void test_loops() {
		test("do a() while (b)",
				"L1, a, callN, POP, L2, b, bool, IFTRUE L1, L3");
//				"L1, a, callN, POP, L2, b, bool, IFTRUE L1, L3");
		test("do a() while (b = c)",
				"L1, a, callN, POP, L2, &b, c, DUP_X2, AASTORE, bool, IFTRUE L1, L3");

		test("while (a) b()",
				"GOTO L1, L2, b, callN, POP, L1, a, bool, IFTRUE L2, L3");
//				"L1, a, bool, IFFALSE L2, b, callN, POP, GOTO L1, L2");
		test("while (a = b) c()",
				"GOTO L1, L2, c, callN, POP, L1, &a, b, DUP_X2, AASTORE, bool, IFTRUE L2, L3");
//				"L1, &a, b, DUP_X2, AASTORE, bool, IFFALSE L2, c, callN, POP, GOTO L1, L2");

		test("forever a()",
				"L1, a, callN, POP, GOTO L1, L2");

		test("for(;;) a()",
				"L1, a, callN, POP, GOTO L1, L2");
		test("for(b();;) a()",
				"b, callN, POP, L1, a, callN, POP, GOTO L1, L2");
		test("for(b();c;) a()",
				"b, callN, POP, GOTO L1, L2, a, callN, POP, L1, c, bool, IFTRUE L2, L3");
//				"b, callN, POP, L1, c, bool, IFFALSE L2, a, callN, POP, GOTO L1, L2");
		test("for(; a = b;) c()",
				"GOTO L1, L2, c, callN, POP, L1, &a, b, DUP_X2, AASTORE, bool, IFTRUE L2, L3");
//				"L1, &a, b, DUP_X2, AASTORE, bool, IFFALSE L2, c, callN, POP, GOTO L1, L2");
		test("for(b();c;a()) A()",
				"b, callN, POP, GOTO L1, L2, 'A', global, callN, POP, a, callN, POP, L1, c, bool, IFTRUE L2, L3");
//				"b, callN, POP, GOTO L1, L2, a, callN, POP, L1, c, bool, IFFALSE L3, A, callN, POP, GOTO L2, L3");
		test("for (a = 0; a < 4; ++a) b()",
				"&a, 0, AASTORE, GOTO L1, "
				+ "L2, b, callN, POP, &a, DUP2, AALOAD, add1, AASTORE, "
				+ "L1, a, 4, lt_, IFTRUE L2, L3");
//				"&a, 0, AASTORE, GOTO L1, "
//				+ "L2, &a, DUP2, AALOAD, add1, AASTORE, "
//				+ "L1, a, 4, lt_, IFFALSE L3, b, callN, POP, GOTO L2, L3");

		test("forever { a(); break; b() }",
				"L1, a, callN, POP, GOTO L2, b, callN, POP, GOTO L1, L2");
		test("forever { a(); continue; b() }",
				"L1, a, callN, POP, GOTO L1, b, callN, POP, GOTO L1, L2");
		test("do { a(); break; b() } while (c)",
				"L1, a, callN, POP, GOTO L2, b, callN, POP, L3, c, bool, IFTRUE L1, L2");
		test("do { a(); continue; b() } while (c)",
				"L1, a, callN, POP, GOTO L2, b, callN, POP, L2, c, bool, IFTRUE L1, L3");

		test("for (a in b) c()",
				"b, iterator, ASTORE 4, GOTO L1, " +
				"L2, ALOAD 4, next, args, SWAP, 0, SWAP, AASTORE, c, callN, POP, " +
				"L1, ALOAD 4, hasNext, IFTRUE L2, L3");
//				"b, iterator, ASTORE 4, L1, ALOAD 4, hasNext, IFFALSE L2, " +
//				"ALOAD 4, next, args, SWAP, 0, SWAP, AASTORE, c, callN, POP, GOTO L1, L2");

		compile("for (a in b) try c() catch ;");
	}
	@Test public void test_switch() {
		test("switch (a) { }",
				"a, ASTORE 4");
		test("switch (a) { default: b() }",
				"a, ASTORE 4, b, callN, POP, L1, L2");
//				"a, ASTORE 4, b, callN, POP");
		test("switch (a) { case 123: b() }",
				"a, ASTORE 4, 123, ALOAD 4, is_, IFFALSE L1, L2, b, callN, POP, L1, L3");
//				"a, ASTORE 4, 123, ALOAD 4, is_, IFFALSE L1, b, callN, POP, L1");
		test("switch (a = b) { case 123: b() }",
				"&a, b, DUP_X2, AASTORE, ASTORE 4, 123, ALOAD 4, is_, IFFALSE L1, L2, b, callN, POP, L1, L3");
//				"&a, b, DUP_X2, AASTORE, ASTORE 4, 123, ALOAD 4, is_, IFFALSE L1, b, callN, POP, L1");
		test("switch (a) { case 123,456: b() }",
				"a, ASTORE 4, 123, ALOAD 4, is_, IFTRUE L1, 456, ALOAD 4, is_, IFFALSE L2, L1, b, callN, POP, L2, L3");
//				"a, ASTORE 4, 123, ALOAD 4, is_, IFTRUE L1, 456, ALOAD 4, is_, IFFALSE L2, L1, b, callN, POP, L2");
	}
	@Test public void test_exceptions() {
		test("throw 'oops'",
				"'oops', throw");
		test("throw a",
				"a, throw");
		test("throw a = b",
				"&a, b, DUP_X2, AASTORE, throw");
		test_e("try a()",
				"try L0 L1 L2, L3, L0, a, callN, POP, L1, GOTO L4, L2, POP, L4");
		test_e("try a() catch ;",
				"try L0 L1 L2, L3, L0, a, callN, POP, L1, GOTO L4, L2, POP, L4");
		test_e("try a() catch b()",
				"try L0 L1 L2, L3, L0, a, callN, POP, L1, GOTO L4, L2, POP, b, callN, POP, L4");
		test_e("try a() catch(e) b()",
				"try L0 L1 L2, L3, L0, a, callN, POP, L1, GOTO L4, L2, toString, " +
					"args, SWAP, 3, SWAP, AASTORE, b, callN, POP, L4");
		test_e("try a() catch(e, 'x') b()",
				"try L0 L1 L2, L3, L0, a, callN, POP, L1, GOTO L4, L2, " +
					"'x', catchMatch, args, SWAP, 3, SWAP, AASTORE, b, callN, POP, L4");
	}
	@Test public void test_block() {
		test_b0("Do() { this }",
				"self, ARETURN");
		test_b0("Do() { .a + .b }",
				"self, 'a', getMem, self, 'b', getMem, add, ARETURN");
		test_b0("Do() { return 123 }",
				"123, block_return");

		test_e("b = { }",
				"try L0 L1 L2, L3, &b, block, L0, DUP_X2, AASTORE, ARETURN, "
				+ "L1, L2, DUP, .locals, args, IF_ACMPEQ L4, "
				+ "ATHROW, L4, .returnValue, ARETURN");
		compile("Foreach(a, { })");
		compile("Foreach(a) { }");
		compile("Plugins().Foreach(a, { })");
		compile("Plugins.Foreach(a) { }");
		compile("Plugins().Foreach(a) { }");
		compile("b = { .001 }");
		test_e("b = { return 123 }",
				"try L0 L1 L2, L3, &b, block, L0, DUP_X2, AASTORE, ARETURN, "
				+ "L1, L2, DUP, .locals, args, IF_ACMPEQ L4, "
				+ "ATHROW, L4, .returnValue, ARETURN");
	}
	@Test public void test_block_break() {
		compile("b = { break }");
		compile("Foreach(a, { break })");
		compile("Foreach(a) { break }");
		compile("Plugins().Foreach(a, { break })");
		compile("Plugins().Foreach(a) { break }");
	}
	@Test public void test_const_named_args() {
		test("x(0, 1, 2, a: 3, b: 4, c: 5)",
			"x, 0, 1, 2, NAMED, 'a', 3, NAMED, 'b', 4, NAMED, 'c', 5, callN, ARETURN");
		test("x(a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11)",
			"x, EACH, " +
			"0=#(f: 6, g: 7, d: 4, e: 5, b: 2, c: 3, a: 1, j: 10, k: 11, h: 8, i: 9), " +
			"callN, ARETURN");
		test("x('', y(a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11))",
			"x, '', y, EACH, " +
			"0=#(f: 6, g: 7, d: 4, e: 5, b: 2, c: 3, a: 1, j: 10, k: 11, h: 8, i: 9), " +
			"callN, null?, callN, ARETURN");
		test("x(a: 1, b: 2, c: 3, d: 4, e: 5, V: a, f: 6, g: 7, h: 8, i: 9, j: 10, k: 11)",
			"x, NAMED, 'V', a, EACH, " +
			"0=#(f: 6, g: 7, d: 4, e: 5, b: 2, c: 3, a: 1, j: 10, k: 11, h: 8, i: 9), " +
			"callN, ARETURN");
	}

	@Test
	public void test_super() {
		try {
			test("a = super.y", "");
			fail("invalid use of super");
		} catch (SuException e) {
			assertEquals("syntax error at line 1: invalid use of super", e.toString());
		}
	}

//	@Test public void test_compile_builtin_calls() {
//		test("Object()", "");
//	}

	private void test(String expr, String expected) {
		test(expr, expected,
				"call(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n   L0\n");
	}
	private void test_e(String expr, String expected) {
		test(expr, expected,
				"call(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n");
	}
	private void test_b0(String expr, String expected) {
		String s = compile(expr);
		s = simplify(s,
				"eval(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n   L0");
		s = before(s, ", INVOKESPECIAL suneido/language/BlockReturnException.<init>");
		assertEquals(expr, expected, s);
	}
	private void test(String expr, String expected, String after) {
		String s = compile(expr);
		s = after(s, "class suneido/language/Test ");
		s = simplify(s, after);
		assertEquals(expr, expected, s);
	}

	private Object[] constants;

	private String compile(String s) {
		//System.out.println("====== " + s);
		if (!s.startsWith("class") && !s.startsWith("function")
				&& !s.startsWith("#("))
			s = "function (a,b,c) { " + s + " }";
		Lexer lexer = new Lexer(s);
		StringWriter sw = new StringWriter();
//		CompileGenerator generator =
//				new CompileGenerator("Test", new PrintWriter(sw));
//		ParseConstant<Object, Generator<Object>> pc =
//				new ParseConstant<Object, Generator<Object>>(lexer, generator);
//		SuCallable x = (SuCallable) pc.parse();

		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		PrintWriter pw = new PrintWriter(sw);
		SuCallable x = (SuCallable) new AstCompile("Test", pw).fold(ast);

		constants = x.constants == null ? new Object[0] : x.constants;
		return sw.toString();
	}

	private String simplify(String r, String after) {
		//System.out.println(r);
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
			{ "ALOAD 1", "self" },
			{ "ALOAD 2", "args" },
			{ "ALOAD 3", "const" },
			{ "this, GETFIELD suneido/language/Test.params : Lsuneido/language/FunctionSpec;, ", "" },
			{ "args, INVOKESTATIC suneido/language/Args.massage (Lsuneido/language/FunctionSpec;[Object;)[Object;, ASTORE 2, ", "" },
			{ "this, GETFIELD suneido/language/Test.constants : [Object;, ASTORE 3, ", "" },
			{ "this, GETFIELD suneido/language/Test$b.constants : [Object;, ASTORE 3, ", "" },
			{ "args, ICONST_0, AALOAD", "a" },
			{ "args, ICONST_1, AALOAD", "b" },
			{ "args, ICONST_2, AALOAD", "c" },
			{ "args, ICONST_3, AALOAD", "x" },
			{ "args, ICONST_4, AALOAD", "y" },
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
			{ "INVOKESTATIC suneido/language/Globals.get (String;)Object;", "global" },
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
			{ " (Object;Object;Object;Object;Object;Object;Object;Object;Object;Object;Object;Object;Object;)Object;", "" },
			{ "DUP, IFNONNULL L1, LDC 'no return value', thrower (Object;)V, L1", "null?" },
			{ "DUP, IFNONNULL L1, LDC 'uninitialized variable', thrower (Object;)V, L1", "null?" },
			{ "DUP, IFNONNULL L2, LDC 'uninitialized variable', thrower (Object;)V, L2", "null?" },
			{ "DUP, IFNONNULL L3, LDC 'uninitialized variable', thrower (Object;)V, L3", "null?" },
			{ "DUP, IFNONNULL L3, LDC 'no value', thrower (Object;)V, L3", "null?" },
			{ "LDC 'Test', INVOKESTATIC suneido/language/Constants.get (LString;)[LSuValue;, DUP, ASTORE 2", "const" },
			{ "const, 0, AALOAD", "0=" + (constants.length > 0 ? display(constants[0]) : "") },
			{ "const, 1, AALOAD", "1=" + (constants.length > 1 ? display(constants[1]) : "") },
			{ "const, 2, AALOAD", "2=" + (constants.length > 2 ? display(constants[2]) : "") },
			{ "toIntBool (Object;)I", "bool" },
			{ "IFEQ", "IFFALSE" },
			{ "IFNE", "IFTRUE" },
			{ "NEW suneido/language/SuBlock, DUP, 0=aTest$b, self, args, INVOKESPECIAL suneido/language/SuBlock.<init> (Object;Object;[Object;)V", "block" },
			{ " INVOKESTATIC java/lang/Integer.valueOf (I)Integer;,", "" },
			{ "BIPUSH ", "" },
			{ "SIPUSH ", "" },
			{ "LDC ", "" },
			{ "thrower (Object;)V", "throw" },
			{ "TRYCATCHBLOCK L0 L1 L2 suneido/SuException", "try L0 L1 L2" },
			{ "TRYCATCHBLOCK L0 L1 L2 suneido/language/BlockReturnException", "try L0 L1 L2" },
			{ "catchMatch (Lsuneido/SuException;String;)String;", "catchMatch" },
			{ "INVOKEVIRTUAL suneido/SuException.toString ()String;", "toString" },
			{ "GETFIELD suneido/language/BlockReturnException.returnValue : Object;", ".returnValue" },
			{ "GETFIELD suneido/language/BlockReturnException.locals : [Object;", ".locals" },
			{ "INVOKEVIRTUAL suneido/language/SuCallable.superInvokeN", "superInvokeN" },
			{ "GETSTATIC java/lang/Boolean.TRUE : Boolean;", "true" },
			{ "GETSTATIC java/lang/Boolean.FALSE : Boolean;", "false" },
			{ "INVOKESTATIC suneido/language/ArgArray.buildN ()[Object;, ", "" },
			{ "INVOKESTATIC suneido/language/ArgArray.buildN (Object;Object;)[Object;, ", "" },
			{ "INVOKESTATIC suneido/language/builtin/ObjectClass.create ([Object;)Object;", "Object" },
			{ "NEW suneido/language/BlockReturnException, DUP_X1, SWAP, args", "block_return" },
		};
		for (String[] simp : simplify)
			r = r.replace(simp[0], simp[1]);
		return r;
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
