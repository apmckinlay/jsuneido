package suneido.language;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

import suneido.SuValue;


public class CompileGeneratorExpressionTest {

	@Test
	public void test() {
		String[][] cases = {
				{ "return", "null, ARETURN" },
				{ "123", "0=123, ARETURN" },
				{ "return 123", "0=123, ARETURN" },
				{ "b;;", "b, POP" },
				{ "b", "b, null?, ARETURN" },
				{ "return b", "b, null?, ARETURN" },
				{ "return a + b", "a, b, .add, ARETURN" },
				{ "a()", "a, .invokeN, ARETURN" },
				{ "a(b, c)", "a, b, c, .invokeN, ARETURN" },
				{ "a.Size()", "a, LDC 'Size', .invokeN, ARETURN" },
				{ "return a.Size()", "a, LDC 'Size', .invokeN, ARETURN" },
				{ "a.Substr(b, c)", "a, LDC 'Substr', b, c, .invokeN, ARETURN" },
				{ "a = b $ c", "&a, b, c, .cat, DUP_X2, AASTORE, ARETURN" },
				{ "a = b = c", "&a, &b, c, null?, DUP_X2, AASTORE, DUP_X2, AASTORE, ARETURN" },
				{ "a = b; return c", "&a, b, null?, AASTORE, c, null?, ARETURN" },
				{ "a = b = c; return c", "&a, &b, c, null?, DUP_X2, AASTORE, AASTORE, c, null?, ARETURN" },
				{ "return this", "this, ARETURN" },
				{ "a += b;;", "&a, b, a, .add, AASTORE" },
				{ "a *= b;;", "&a, b, a, .mul, AASTORE" },
				{ "++a;;", "&a, a, .add1, AASTORE" },
				{ "--a;;", "&a, a, .sub1, AASTORE" },
				{ "a++", "&a, DUP2, AALOAD, DUP_X2, .add1, AASTORE, ARETURN" },
				{ "a.x", "a, LDC 'x', .getMem, ARETURN" },
				{ ".x", "this, LDC 'x', .getMem, ARETURN" },
				{ "a.x = b;;", "a, LDC 'x', b, .putMem" },
				{ "a.x = b", "a, LDC 'x', b, DUP_X2, .putMem, ARETURN" },
				{ "a[b]", "a, b, .getSub, ARETURN" },
				{ "a[b] = c;;", "a, b, c, .putSub" },
				{ "G", "LDC 'G', global, ARETURN" },
				{ "G()", "LDC 'G', global, .invokeN, ARETURN" },
				{ "a(@b)", "a, EACH, b, .invokeN, ARETURN" },
				{ "a(@+1b)", "a, EACH1, b, .invokeN, ARETURN" },
				{ "a = b();;", "&a, b, .invokeN, null?, AASTORE" },
				{ "123; 456; 123;", "0=123, POP, 1=456, POP, 0=123, ARETURN" },
				{ "#(1, a: 2)", "0=#(1, a: 2), ARETURN" },
				{ "#{1, a: 2}", "0=[1, a: 2], ARETURN" },
				{ "a(1, x: 2)", "a, 0=1, NAMED, 1=2, .invokeN, ARETURN" },
		};
		for (String[] c : cases) {
			assertEquals(c[0], c[1], compile(c[0]));
		}
	}

	private String after(String r, String s) {
		int i = r.indexOf(s);
		return r.substring(i + s.length(), r.length());
	}
	private String before(String r, String s) {
		int i = r.indexOf(s);
		return r.substring(0, i);
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
		r = after(r, "invoke([Lsuneido/SuValue;)Lsuneido/SuValue;\n   L0\n");
		r = before(r, "    LOCALVARIABLE");
		r = r.substring(0, r.length() - 6); // label
System.out.println(r);
		r = r.trim();
		r = r.replace("\n", ", ");
		r = r.replace('"', '\'');
		r = r.replaceAll(" +", " ");
		SuValue[] constants = generator.constants == null
			? new SuValue[0]
			: generator.constants.get(0);
		String[][] simplify = {
				{ "ALOAD 1, ICONST_0, AALOAD", "a" },
				{ "ALOAD 1, ICONST_1, AALOAD", "b" },
				{ "ALOAD 1, ICONST_2, AALOAD", "c" },
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
				{ "suneido/SuValue", "SuValue" },
				{ "suneido/SuString", "SuString" },
				{ "suneido/SuNumber", "SuNumber" },
				{ "java/lang/String", "String" },
				{ "ANEWARRAY SuValue", "new SuValue[]" },
				{ "GETSTATIC suneido/language/MyFunc.constants : [[LSuValue;, BIPUSH 0, AALOAD, DUP, ASTORE 2", "const" },
				{ "GETSTATIC suneido/language/SuClass.", "" },
				{ " : LSuString;", "" },
				{ "INVOKESTATIC suneido/language/Globals.get (LString;)LSuValue;", "global" },
				{ "INVOKEVIRTUAL SuValue", "" },
				{ ".get (LSuValue;)LSuValue;", ".getSub" },
				{ ".get (LString;)LSuValue;", ".getMem" },
				{ ".put (LString;LSuValue;)V", ".putMem" },
				{ ".put (LSuValue;LSuValue;)V", ".putSub" },
				{ " ()LSuValue;", "" },
				{ " (LSuValue;)LSuValue;", "" },
				{ " (LSuValue;)LSuString;", "" },
				{ " (LSuValue;)LSuNumber;", "" },
				{ " (LSuValue;LSuValue;)LSuValue;", "" },
				{ " (LSuValue;LSuValue;LSuValue;)LSuValue;", "" },
				{ " (LString;)LSuValue;", "" },
				{ " (LString;LSuValue;)LSuValue;", "" },
				{ " (LString;LSuValue;LSuValue;)LSuValue;", "" },
				{ " (LString;LSuValue;LSuValue;LSuValue;)LSuValue;", "" },
				{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'no return value', INVOKESPECIAL suneido/SuException.<init> (LString;)V, ATHROW, L1", "null?" },
				{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (LString;)V, ATHROW, L1", "null?" },
				{ "DUP, IFNONNULL L2, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (LString;)V, ATHROW, L2", "null?" },
				{ "LDC 'MyFunc', INVOKESTATIC suneido/language/Constants.get (LString;)[LSuValue;, DUP, ASTORE 2", "const" },
				{ "const, 0, AALOAD", "0=" + (constants.length > 0 ? constants[0] : "") },
				{ "const, 1, AALOAD", "1=" + (constants.length > 1 ? constants[1] : "") },
		};
		for (String[] simp : simplify)
			r = r.replace(simp[0], simp[1]);
		return r;
	}



}
