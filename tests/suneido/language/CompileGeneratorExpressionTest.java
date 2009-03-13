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
				{ "return b", "b, ARETURN" },
				{ "return a + b", "a, b, .add, ARETURN" },
				{ "a.Size()", "a, LDC 'Size', 0, new SuValue[], invoke, ARETURN" },
				{ "return a.Size()", "a, LDC 'Size', 0, new SuValue[], invoke, ARETURN" },
				{ "a.Substr(b, c)", "a, b, c, LDC 'Substr', 2, new SuValue[], invoke, ARETURN" },
				{ "a = b $ c", "&a, b, c, .cat, DUP_X2, AASTORE, ARETURN" },
				{ "a = b = c", "&a, &b, c, DUP_X2, AASTORE, DUP_X2, AASTORE, ARETURN" },
				{ "a = b; return c", "&a, b, AASTORE, c, ARETURN" },
				{ "a = b = c; return c", "&a, &b, c, DUP_X2, AASTORE, AASTORE, c, ARETURN" },
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
		i = r.lastIndexOf("   L1\n");
		r = r.substring(0, i);
System.out.println(r);
		r = r.trim();
		r = r.replace("\n", ", ");
		r = r.replace('"', '\'');
		r = r.replaceAll(" +", " ");
		String[][] simplify = {
				{ "ALOAD 1, ICONST_0, AALOAD", "a" },
				{ "ALOAD 1, ICONST_1, AALOAD", "b" },
				{ "ALOAD 1, ICONST_2, AALOAD", "c" },
				{ "ALOAD 1, ICONST_0", "&a" }, { "ALOAD 1, ICONST_1", "&b" }, { "ALOAD 1, ICONST_2", "&c" },
				{ "ALOAD 0", "this" },
				{ "ALOAD 1", "vars" },
				{ "ALOAD 2", "const" },
				{ "ICONST_0", "0" },
				{ "ICONST_1", "1" },
				{ "ICONST_2", "2" },
				{ "ACONST_NULL", "null" },
				{ "suneido/SuValue", "SuValue" },
				{ "ANEWARRAY SuValue", "new SuValue[]" },
				{ "INVOKEVIRTUAL SuValue.invoke (Ljava/lang/String;[LSuValue;)LSuValue;", "invoke" },
				{ "INVOKEVIRTUAL SuValue", "" },
				{ " (LSuValue;)LSuValue;", "" },
		};
		for (String[] simp : simplify)
			r = r.replace(simp[0], simp[1]);
		return r;
	}



}
