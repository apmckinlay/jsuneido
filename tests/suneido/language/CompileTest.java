package suneido.language;

import static org.junit.Assert.assertEquals;
import static suneido.language.Ops.display;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

public class CompileTest {

	public void tmp() {
		String s = "function (a,b,c) { if (a < b) c }";
		Lexer lexer = new Lexer(s);
		CompileGenerator generator =
				new CompileGenerator(new PrintWriter(System.out));
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		pc.parse();
	}

	@Test
	public void tests() {
		test("return",
				"null, ARETURN");
		test("123",
				"0=123, ARETURN");
		test("return 123",
 				"0=123, ARETURN");
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
		test("a()",
 				"a, invokeN, ARETURN");
		test("a(b, c)",
 				"a, b, c, invokeN, ARETURN");
		test("a.Size()",
 				"a, LDC 'Size', invokeN, ARETURN");
		test("return a.Size()",
 				"a, LDC 'Size', invokeN, ARETURN");
		test("a.Substr(b, c)",
 				"a, LDC 'Substr', b, c, invokeN, ARETURN");
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
		test("return this",
 				"this, ARETURN");
		test("a += b;;",
 				"&a, b, a, add, AASTORE");
		test("a *= b;;",
 				"&a, b, a, mul, AASTORE");
		test("++a;;",
 				"&a, DUP2, AALOAD, add1, DUP_X2, AASTORE, POP");
		test("--a;;",
 				"&a, DUP2, AALOAD, sub1, DUP_X2, AASTORE, POP");
		test("a++",
 				"&a, DUP2, AALOAD, DUP_X2, add1, AASTORE, ARETURN");
		test("a.x",
 				"a, LDC 'x', getMem, ARETURN");
		test(".x",
 				"this, LDC 'x', getMem, ARETURN");
		test("a.x = b;;",
 				"a, LDC 'x', b, putMem");
		test("a.x = b",
 				"a, LDC 'x', b, DUP_X2, putMem, ARETURN");
		test("a[b]",
 				"a, b, getMem, ARETURN");
		test("a[b] = c;;",
 				"a, b, c, putMem");
		test("G",
 				"LDC 'G', global, ARETURN");
		test("G()",
 				"LDC 'G', global, invokeN, ARETURN");
		test("a(@b)",
 				"a, EACH, b, invokeN, ARETURN");
		test("a(@+1b)",
 				"a, EACH1, b, invokeN, ARETURN");
		test("a = b();;",
 				"&a, b, invokeN, null?, AASTORE");
		test("123; 456; 123;",
 				"0=123, POP, 1=456, POP, 0=123, ARETURN");
		test("#(1, a: 2)",
 				"0=#(1, a: 2), ARETURN");
		test("#{1, a: 2}",
 				"0=[1, a: 2], ARETURN");
		test("a(1, x: 2)",
 				"a, 0=1, NAMED, 1='x', 2=2, invokeN, ARETURN");
		test("if (a) b",
				"a, bool, IFFALSE L1, b, POP, L1");
		test("if (a < b) c",
				"a, b, lt_, IFFALSE L1, c, POP, L1");
		test("if (a) b else c",
				"a, bool, IFFALSE L1, b, POP, GOTO L2, L1, c, POP, L2");
		test("do a while (b)",
				"L1, a, POP, b, bool, IFTRUE L1, L2");
		test("while (a) b",
				"L1, a, bool, IFFALSE L2, b, POP, GOTO L1, L2");
		test("forever a",
				"L1, a, POP, GOTO L1, L2");
		test("for(;;) a",
				"L1, a, POP, GOTO L1, L2");
		test("for(b;;) a",
				"b, POP, L1, a, POP, GOTO L1, L2");
		test("for(b;c;) a",
				"b, POP, L1, c, bool, IFFALSE L2, a, POP, GOTO L1, L2");
		test("for(b;c;a) a",
				"b, POP, GOTO L1, L2, a, POP, L1, c, bool, IFFALSE L3, a, POP, GOTO L2, L3");
		test("forever { a; break; b }",
				"L1, a, POP, GOTO L2, b, POP, GOTO L1, L2");
		test("forever { a; continue; b }",
				"L1, a, POP, GOTO L1, b, POP, GOTO L1, L2");
		test("switch (a) { }",
				"a, POP, L1");
		test("switch (a) { default: b }",
				"a, POP, b, POP, GOTO L1, POP, L1");
		test("switch (a) { case 0: b }",
				"a, DUP, 0=0, is_, IFFALSE L1, POP, b, POP, GOTO L2, L1, POP, L2");
		test("switch (a) { case 0,1: b }",
				"a, DUP, 0=0, is_, IFTRUE L1, DUP, 1=1, is_, IFFALSE L2, L1, POP, b, POP, GOTO L3, L2, POP, L3");
	}

	private void test(String expr, String expected) {
		assertEquals(expr, expected, compile(expr));
	}

	private String compile(String s) {
System.out.println("====== " + s);
		s = "function (a,b,c) { " + s + " }";
		Lexer lexer = new Lexer(s);
		StringWriter sw = new StringWriter();
		CompileGenerator generator = new CompileGenerator(new PrintWriter(sw));
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		pc.parse();
		String r = sw.toString();
		r = after(r, "invoke([Ljava/lang/Object;)Ljava/lang/Object;\n   L0\n");
		r = before(r, "    LOCALVARIABLE");
System.out.println(r);
		r = r.substring(0, r.length() - 6); // label
		r = r.trim();
		r = r.replace("\n", ", ");
		r = r.replace('"', '\'');
		r = r.replaceAll(" +", " ");
		Object[] constants =
				generator.constants == null ? new Object[0]
			: generator.constants.get(0);
		String[][] simplify = {
				{ "GETSTATIC suneido/language/MyFunc.constants : [[Ljava/lang/Object;, ICONST_0, AALOAD, ASTORE 2, ", "" },
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
				{ "ICONST_0", "0" },
				{ "ICONST_1", "1" },
				{ "ICONST_2", "2" },
				{ ", ACONST_NULL, ARETURN", "" },
				{ "ACONST_NULL", "null" },
				{ "Ljava/lang/", "" },
				{ "ANEWARRAY Object", "new Object[]" },
				{ "GETSTATIC suneido/language/SuClass.", "" },
				{ " : LString;", "" },
				{ "GETSTATIC suneido/language/SuClass$SpecialArg.", "" },
				{ " : Lsuneido/language/SuClass$SpecialArg;", "" },
				{ "INVOKESTATIC suneido/language/Globals.get (String;)Object;", "global" },
				{ "INVOKESTATIC suneido/language/Ops.", "" },
				{ "get (Object;Object;)Object;", "getMem" },
				{ "put (Object;Object;Object;)V", "putMem" },
				{ " (Object;)Object;", "" },
				{ " (Object;)Number;", "" },
				{ " (Object;Object;)Z", "" },
				{ " (Object;Object;)Number;", "" },
				{ " (Object;Object;)String;", "" },
				{ " (Object;Object;)Boolean;", "" },
				{ " (Object;Object;Object;)Object;", "" },
				{ " (Object;String;)Object;", "" },
				{ " (Object;String;Object;Object;)Object;", "" },
				{ " (Object;Object;Object;Object;Object;)Object;", "" },
				{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'no return value', INVOKESPECIAL suneido/SuException.<init> (String;)V, ATHROW, L1", "null?" },
				{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (String;)V, ATHROW, L1", "null?" },
				{ "DUP, IFNONNULL L2, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (String;)V, ATHROW, L2", "null?" },
				{ "LDC 'MyFunc', INVOKESTATIC suneido/language/Constants.get (LString;)[LSuValue;, DUP, ASTORE 2", "const" },
				{ "const, 0, AALOAD", "0=" + (constants.length > 0 ? display(constants[0]) : "") },
				{ "const, 1, AALOAD", "1=" + (constants.length > 1 ? display(constants[1]) : "") },
				{ "const, 2, AALOAD", "2=" + (constants.length > 2 ? display(constants[2]) : "") },
				{ "toBool (Object;)I", "bool" },
				{ "IFEQ", "IFFALSE" },
				{ "IFNE", "IFTRUE" },
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
