package suneido.language;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;


public class CompileGeneratorExpressionTest {
	private final StringWriter sw = new StringWriter();

	@Test
	public void test() {
		String[][] cases = {
				{ "return", "null, ARETURN" },
				{ "123", "const, 0, AALOAD, ARETURN" },
				{ "return 123", "const, 0, AALOAD, ARETURN" },
				{ "b;;", "b, POP" },
				{ "b", "b, null?, ARETURN" },
				{ "return b", "b, null?, ARETURN" },
				{ "return a + b", "a, b, .add, ARETURN" },
				{ "a()", "a, .invokeN, ARETURN" },
				{ "a(b, c)", "a, b, c, .invokeN, ARETURN" },
				{ "a.Size()", "a, LDC 'Size', .invokeN, ARETURN" },
				{ "return a.Size()", "a, LDC 'Size', .invokeN, ARETURN" },
				{ "a.Substr(b, c)", "a, b, c, LDC 'Substr', .invokeN, ARETURN" },
				{ "a = b $ c", "&a, b, c, .cat, DUP_X2, AASTORE, ARETURN" },
				{ "a = b = c", "&a, &b, c, null?, DUP_X2, AASTORE, DUP_X2, AASTORE, ARETURN" },
				{ "a = b; return c", "&a, b, null?, AASTORE, c, null?, ARETURN" },
				{ "a = b = c; return c", "&a, &b, c, null?, DUP_X2, AASTORE, AASTORE, c, null?, ARETURN" },
				{ "return this", "this, ARETURN" },
				{ "a += b;;", "&a, b, a, .add, AASTORE" },
				{ "a *= b;;", "&a, b, a, .mul, AASTORE" },
				{ "++a;;", "&a, a, .add1, AASTORE" },
				{ "--a;;", "&a, a, .sub1, AASTORE" },
				{ "a++", "a, &a, .add1, DUP_X2, AASTORE, ARETURN" },
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
		};
		for (String[] c : cases) {
			assertEquals(c[0], c[1], compile(c[0]));
		}
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
		int i = r.indexOf("ASTORE 2\n");
		r = r.substring(i + 9, r.length());
		i = r.indexOf("    LOCALVARIABLE");
		r = r.substring(0, i - 6);
System.out.println(r);
		r = r.trim();
		r = r.replace("\n", ", ");
		r = r.replace('"', '\'');
		r = r.replaceAll(" +", " ");
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
				{ "java/lang/String", "String" },
				{ "ANEWARRAY SuValue", "new SuValue[]" },
				{ "GETSTATIC suneido/language/SuClass.", "" },
				{ " : Lsuneido/SuString;", "" },
				{ "INVOKESTATIC suneido/language/Globals.get (LString;)LSuValue;", "global" },
				{ "INVOKEVIRTUAL SuValue", "" },
				{ ".get (LSuValue;)LSuValue;", ".getSub" },
				{ ".get (LString;)LSuValue;", ".getMem" },
				{ ".put (LString;LSuValue;)V", ".putMem" },
				{ ".put (LSuValue;LSuValue;)V", ".putSub" },
				{ " ()LSuValue;", "" },
				{ " (LSuValue;)LSuValue;", "" },
				{ " (LSuValue;LSuValue;)LSuValue;", "" },
				{ " (LSuValue;LSuValue;LSuValue;)LSuValue;", "" },
				{ " (LString;)LSuValue;", "" },
				{ " (LString;LSuValue;)LSuValue;", "" },
				{ " (LString;LSuValue;LSuValue;)LSuValue;", "" },
				{ " (LString;LSuValue;LSuValue;LSuValue;)LSuValue;", "" },
				{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'no return value', INVOKESPECIAL suneido/SuException.<init> (LString;)V, ATHROW, L1", "null?" },
				{ "DUP, IFNONNULL L1, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (LString;)V, ATHROW, L1", "null?" },
				{ "DUP, IFNONNULL L2, NEW suneido/SuException, DUP, LDC 'uninitialized variable', INVOKESPECIAL suneido/SuException.<init> (LString;)V, ATHROW, L2", "null?" },
		};
		for (String[] simp : simplify)
			r = r.replace(simp[0], simp[1]);
		return r;
	}



}
