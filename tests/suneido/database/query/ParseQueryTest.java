package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuException;
import suneido.language.Lexer;


public class ParseQueryTest {
	@Test
	public void test() {
		String[][] cases = new String[][] {
				{ "insert t1 into t2", null },
				{ "insert { a: 1, b: '$' } into t",
						"insert #{s(a): n(1), s(b): s($)} into t" },
				{ "update t set a = 1, b = '$'",
						"update t set a = n(1), b = s($)" },
				{ "delete mytable", null },
				{ "mytable", null },
				{ "history(x)", null },
				{ "(mytable)", "mytable" },
				{ "a sort b, c", null },
				{ "a sort reverse b, c", null },
				{ "a project b, c", null },
				{ "a project b project c", null },
				{ "a remove b, c", null },
				{ "a remove b remove c", null },
				{ "a times b", null },
				{ "a times b times c", null },
				{ "a union b", null },
				{ "a union b union c", null },
				{ "a minus b", null },
				{ "a minus b minus c", null },
				{ "a intersect b", null },
				{ "a intersect b intersect c", null },
				{ "a join by(x, y) b", null },
				{ "a join b join c", null },
				{ "a leftjoin by(x, y) b", null },
				{ "a leftjoin b leftjoin c", null },
				{ "a rename b to c, d to e", null },
				{ "a rename b to c rename d to e", null },
				{ "x extend a = 1, b = '$'",
						"x extend a = n(1), b = s($)" },
				{ "x where a = 1 and b = '$'",
						"x where a n(1) IS b s($) IS AND" },
				{ "a summarize count", null },
				{ "a summarize b, c, total d, max e", null },
		};
		for (String[] c : cases) {
			// System.out.println(c[0]);
			assertEquals(c[1] == null ? c[0] : c[1], parse(c[0]));
		}
	}

	@Test(expected = SuException.class)
	public void bad() {
		parse("");
	}

	@Test(expected = SuException.class)
	public void empty_join_by() {
		parse("a join by() b");
	}

	@Test(expected = SuException.class)
	public void empty_leftjoin_by() {
		parse("a leftjoin by() b");
	}

	private static String parse(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		StringGenerator generator = new StringGenerator();
		ParseQuery<String, QueryGenerator<String>> pc =
				new ParseQuery<String, QueryGenerator<String>>(lexer, generator);
		return pc.parse();
	}

}
