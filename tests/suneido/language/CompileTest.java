package suneido.language;

import static org.junit.Assert.assertEquals;
import static suneido.language.Ops.display;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

public class CompileTest {
	//@Test
	public void blocks() {
		test("return { }",
				"block, ARETURN");
	}

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
		test("return x",
 				"x, null?, ARETURN");
		test("return a + b",
 				"a, b, add, ARETURN");
		test("-a",
				"a, uminus, ARETURN");
		test("! a",
				"a, not, ARETURN");
		test("a()",
 				"a, callN, ARETURN");
		test("a(b, c)",
 				"a, b, c, callN, ARETURN");
		test("a(b = c)",
 				"a, &b, c, DUP_X2, AASTORE, callN, ARETURN");
		test("a(b, x: c)",
				"a, b, NAMED, 'x', c, callN, ARETURN");
		test("a(b, x:)",
				"a, b, NAMED, 'x', true, callN, ARETURN");
		test("a.Size()",
 				"a, 'Size', invokeN, ARETURN");
		test("return a.Size()",
 				"a, 'Size', invokeN, ARETURN");
		test("a.Substr(b, c)",
 				"a, 'Substr', b, c, invokeN, ARETURN");
		test("a = b",
 				"&a, b, DUP_X2, AASTORE, ARETURN");
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
		test("a = x; return x",
				"&a, x, null?, AASTORE, x, null?, ARETURN");
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
		test("(a = b) ? (b = 123) : (c = 456)",
				"&a, b, DUP_X2, AASTORE, bool, IFFALSE L1, "
						+ "&b, 123, DUP_X2, AASTORE, GOTO L2, "
						+ "L1, &c, 456, DUP_X2, AASTORE, L2, ARETURN");
		test("a(b = c)",
				"a, &b, c, DUP_X2, AASTORE, callN, ARETURN");
		test("a[b = c]",
				"a, &b, c, DUP_X2, AASTORE, getMem, ARETURN");
		test("return this",
 				"this, ARETURN");
		test("++a;;",
				"&a, DUP2, AALOAD, add1, AASTORE");
		test("++a",
				"&a, DUP2, AALOAD, add1, DUP_X2, AASTORE, ARETURN");
		test("--a;;",
 				"&a, DUP2, AALOAD, sub1, AASTORE");
		test("a++",
 				"&a, DUP2, AALOAD, DUP_X2, add1, AASTORE, ARETURN");
		test("a--;;",
 				"&a, DUP2, AALOAD, DUP_X2, sub1, AASTORE, POP");
		test("a.x",
 				"a, 'x', getMem, ARETURN");
		test(".x",
			"this, 'Test_x', getMem, ARETURN");
		test(".f()",
			"this, 'Test_f', invokeN, ARETURN");
		test("this.f()",
			"this, 'Test_f', invokeN, ARETURN");
		test("this[a]()",
			"this, a, toMethodString, invokeN, ARETURN");
		test("a.x = b;;",
 				"a, 'x', b, putMem");
		test("a.x = b",
 				"a, 'x', b, DUP_X2, putMem, ARETURN");
		test("(a = b).F()",
				"&a, b, DUP_X2, AASTORE, 'F', invokeN, ARETURN");
		test("a[b]",
 				"a, b, getMem, ARETURN");
		test("a[b] = c;;",
 				"a, b, c, putMem");
		test("a[b] *= c;;",
 				"a, b, DUP2, getMem, c, mul, putMem");
		test("G",
 				"'G', global, ARETURN");
		test("G()",
 				"'G', global, callN, ARETURN");
		test("a(@b)",
 				"a, EACH, b, callN, ARETURN");
		test("a(@+1b)",
 				"a, EACH1, b, callN, ARETURN");
		test("a = b();;",
 				"&a, b, callN, null?, AASTORE");
		test("123; 456; 123;",
 				"123, POP, 456, POP, 123, ARETURN");
		test("#(1, a: 2)",
 				"0=#(1, a: 2), ARETURN");
		test("#{1, a: 2}",
 				"0=[1, a: 2], ARETURN");
		test("a(123, x: 456)",
 				"a, 123, NAMED, 'x', 456, callN, ARETURN");
		test("return function () { }",
				"0=Test.Test_f0, ARETURN");
		test("a = function () { }",
				"&a, 0=Test.Test_f0, DUP_X2, AASTORE, ARETURN");
		test("super.F()",
				"this, this, 'F', superInvokeN, ARETURN");
		test("A().B()",
				"'A', global, callN, 'B', invokeN, ARETURN");
		test("new this",
				"this, '<new>', invokeN, ARETURN");
		test("new this(a)",
				"this, '<new>', a, invokeN, ARETURN");
	}
	@Test public void test_new() {
		test("new c",
				"c, '<new>', invokeN, ARETURN");
		test("new G",
				"'G', global, '<new>', invokeN, ARETURN");
	}
	@Test public void test_if() {
		test("if (a) b",
				"a, bool, IFFALSE L1, b, POP, L1");
		test("if (a < b) c",
				"a, b, lt_, IFFALSE L1, c, POP, L1");
		test("if (a = b) c",
				"&a, b, DUP_X2, AASTORE, bool, IFFALSE L1, c, POP, L1");
		test("if (a) b else c",
				"a, bool, IFFALSE L1, b, POP, GOTO L2, L1, c, POP, L2");
	}
	@Test public void test_loops() {
		test("do a while (b)",
				"L1, a, POP, b, bool, IFTRUE L1, L2");
		test("do a while (b = c)",
				"L1, a, POP, &b, c, DUP_X2, AASTORE, bool, IFTRUE L1, L2");
		test("while (a) b",
				"L1, a, bool, IFFALSE L2, b, POP, GOTO L1, L2");
		test("while (a = b) c",
				"L1, &a, b, DUP_X2, AASTORE, bool, IFFALSE L2, c, POP, GOTO L1, L2");
		test("forever a",
				"L1, a, POP, GOTO L1, L2");
		test("for(;;) a",
				"L1, a, POP, GOTO L1, L2");
		test("for(b;;) a",
				"b, POP, L1, a, POP, GOTO L1, L2");
		test("for(b;c;) a",
				"b, POP, L1, c, bool, IFFALSE L2, a, POP, GOTO L1, L2");
		test("for(; a = b;) c",
				"L1, &a, b, DUP_X2, AASTORE, bool, IFFALSE L2, c, POP, GOTO L1, L2");
		test("for(b;c;a) a",
				"b, POP, GOTO L1, L2, a, POP, L1, c, bool, IFFALSE L3, a, POP, GOTO L2, L3");
		test("for (a = 0; a < 4; ++i) b",
				"&a, 0, AASTORE, GOTO L1, "
				+ "L2, vars, 3, DUP2, AALOAD, add1, AASTORE, "
				+ "L1, a, 4, lt_, IFFALSE L3, b, POP, GOTO L2, L3");
		test("forever { a; break; b }",
				"L1, a, POP, GOTO L2, b, POP, GOTO L1, L2");
		test("forever { a; continue; b }",
				"L1, a, POP, GOTO L1, b, POP, GOTO L1, L2");
		test("for (a in b) c",
				"b, iterator, ASTORE 3, L1, ALOAD 3, hasNext, IFFALSE L2, ALOAD 3, next, vars, SWAP, 0, SWAP, AASTORE, c, POP, GOTO L1, L2");
		compile("for (a in b) try c catch ;");
	}
	@Test public void test_switch() {
		test("switch (a) { }",
				"a, POP, L1");
		test("switch (a) { default: b }",
				"a, POP, b, POP, GOTO L1, POP, L1");
		test("switch (a = b) { case 123: b }",
				"&a, b, DUP_X2, AASTORE, DUP, 123, is_, IFFALSE L1, POP, b, POP, GOTO L2, L1, POP, L2");
		test("switch (a) { case 123,456: b }",
				"a, DUP, 123, is_, IFTRUE L1, DUP, 456, is_, IFFALSE L2, L1, POP, b, POP, GOTO L3, L2, POP, L3");
	}
	@Test public void test_exceptions() {
		test("throw 'oops'",
				"'oops', throw");
		test("throw a",
				"a, throw");
		test("throw a = b",
				"&a, b, DUP_X2, AASTORE, throw");
		test("try 123",
				"L1, 123, POP, L2, GOTO L3, L4, POP, L3, try L1 L2 L4");
		test("try 123 catch 456",
				"L1, 123, POP, L2, GOTO L3, L4, POP, 456, POP, L3, try L1 L2 L4");
		test("try 123 catch(a) 456",
				"L1, 123, POP, L2, GOTO L3, L4, toString, vars, SWAP, 0, SWAP, "
				+ "AASTORE, 456, POP, L3, try L1 L2 L4");
		test("try 123 catch(a, 'x') 456",
				"L1, 123, POP, L2, GOTO L3, L4, 'x', catchMatch, vars, SWAP, "
				+ "0, SWAP, AASTORE, 456, POP, L3, try L1 L2 L4");
	}
	@Test public void test_block() {
		test("b = { }",
				"&b, block, L1, DUP_X2, AASTORE, ARETURN, "
				+ "L2, L3, try L1 L2 L3, DUP, .locals, vars, IF_ACMPEQ L4, "
				+ "ATHROW, L4, .returnValue, ARETURN");
		compile("Foreach(a, { })");
		compile("Foreach(a) { }");
		compile("Plugins().Foreach(a, { })");
		compile("Plugins.Foreach(a) { }");
		compile("Plugins().Foreach(a) { }");
	}
	@Test public void test_block_break() {
		compile("b = { break }");
		compile("Foreach(a, { break })");
		compile("Foreach(a) { break }");
		compile("Plugins().Foreach(a, { break })");
		compile("Plugins().Foreach(a) { break }");
	}

	private void test(String expr, String expected) {
		assertEquals(expr, expected, simplify(compile(expr)));
	}

	private Object[] constants;

	private String compile(String s) {
		//System.out.println("====== " + s);
		if (! s.startsWith("class") && ! s.startsWith("function"))
			s = "function (a,b,c) { " + s + " }";
		Lexer lexer = new Lexer(s);
		StringWriter sw = new StringWriter();
		CompileGenerator generator =
				new CompileGenerator("Test", new PrintWriter(sw));
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		SuCallable x = (SuCallable) pc.parse();
		constants = x.constants == null ? new Object[0] : x.constants[0];
		return sw.toString();
	}

	private String simplify(String r) {
		r = after(r, "call([Ljava/lang/Object;)Ljava/lang/Object;\n   L0\n");
		r = before(r, "    LOCALVARIABLE");
		//System.out.println(r);
		r = r.substring(0, r.length() - 6); // label
		r = r.trim();
		r = r.replace("\n", ", ");
		r = r.replace('"', '\'');
		r = r.replaceAll(" +", " ");
		String[][] simplify = {
			{ "Ljava/lang/", "" },
			{ "ALOAD 0, GETFIELD suneido/language/Test.params : [Lsuneido/language/FunctionSpec;, ICONST_0, AALOAD, ", "" },
			{ "ALOAD 1, INVOKESTATIC suneido/language/Args.massage (Lsuneido/language/FunctionSpec;[Object;)[Object;, ASTORE 1, ", "" },
			{ "ALOAD 0, GETFIELD suneido/language/Test.constants : [[Object;, ICONST_0, AALOAD, ASTORE 2, ", "" },
			{ "ALOAD 1, ICONST_0, AALOAD", "a" },
			{ "ALOAD 1, ICONST_1, AALOAD", "b" },
			{ "ALOAD 1, ICONST_2, AALOAD", "c" },
			{ "ALOAD 1, ICONST_3, AALOAD", "x" },
			{ "ALOAD 1, ICONST_0", "&a" },
			{ "ALOAD 1, ICONST_1", "&b" },
			{ "ALOAD 1, ICONST_2", "&c" },
			{ "ALOAD 0", "this" },
			{ "ALOAD 1", "vars" },
			{ "ALOAD 2", "const" },
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
			{ " (Object;Object;Object;Object;Object;)Object;", "" },
			{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'no return value', INVOKESPECIAL suneido/SuException.<init> (String;)V, ATHROW, L1", "null?" },
			{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (String;)V, ATHROW, L1", "null?" },
			{ "DUP, IFNONNULL L2, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (String;)V, ATHROW, L2", "null?" },
			{ "LDC 'Test', INVOKESTATIC suneido/language/Constants.get (LString;)[LSuValue;, DUP, ASTORE 2", "const" },
			{ "const, 0, AALOAD", "0=" + (constants.length > 0 ? display(constants[0]) : "") },
			{ "const, 1, AALOAD", "1=" + (constants.length > 1 ? display(constants[1]) : "") },
			{ "const, 2, AALOAD", "2=" + (constants.length > 2 ? display(constants[2]) : "") },
			{ "toBool (Object;)I", "bool" },
			{ "IFEQ", "IFFALSE" },
			{ "IFNE", "IFTRUE" },
			{ "NEW suneido/language/SuBlock, DUP, this, DUP, GETFIELD suneido/language/Test.params : [Lsuneido/language/FunctionSpec;, 1, AALOAD, vars, INVOKESPECIAL suneido/language/SuBlock.<init> (Object;Lsuneido/language/FunctionSpec;[Object;)V", "block" },
			{ " INVOKESTATIC java/lang/Integer.valueOf (I)Integer;,", "" },
			{ "BIPUSH ", "" },
			{ "SIPUSH ", "" },
			{ "LDC ", "" },
			{
								"NEW suneido/SuException, DUP_X1, SWAP, INVOKESPECIAL suneido/SuException.<init> (Object;)V, ATHROW",
								"throw" },
			{ "TRYCATCHBLOCK L1 L2 L4 suneido/SuException", "try L1 L2 L4" },
			{ "TRYCATCHBLOCK L1 L2 L3 suneido/language/BlockReturnException", "try L1 L2 L3" },
			{ "catchMatch (Lsuneido/SuException;String;)String;", "catchMatch" },
			{ "INVOKEVIRTUAL suneido/SuException.toString ()String;", "toString" },
			{ "GETFIELD suneido/language/BlockReturnException.returnValue : Object;", ".returnValue" },
			{ "GETFIELD suneido/language/BlockReturnException.locals : [Object;", ".locals" },
			{ "INVOKEVIRTUAL suneido/language/SuFunction.superInvokeN", "superInvokeN" },
			{ "GETSTATIC java/lang/Boolean.TRUE : Boolean;", "true" },
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
		return r.substring(0, i);
	}

}
