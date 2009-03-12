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
				{ "return", "ACONST_NULL, ARETURN" },
				{ "return 123", "ALOAD 2, ICONST_0, AALOAD, ARETURN" },
				{ "return b", "b, ARETURN" },
				{ "return a + b", "a, b, .add, ARETURN" },
				{ "a = b $ c", "ALOAD 1, ICONST_0, b, c, .cat, AASTORE" },
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
		r = r.replaceAll(" +", " ");
		String[][] simplify = {
				{ "ALOAD 1, ICONST_0, AALOAD", "a" },
				{ "ALOAD 1, ICONST_1, AALOAD", "b" },
				{ "ALOAD 1, ICONST_2, AALOAD", "c" },
				{ "INVOKEVIRTUAL suneido/SuValue", "" }, { " (Lsuneido/SuValue;)Lsuneido/SuValue;", "" },
		};
		for (String[] simp : simplify)
			r = r.replace(simp[0], simp[1]);
		return r;
	}



}
