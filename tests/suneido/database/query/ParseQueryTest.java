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
				// TODO insert record
				{ "update t set a = 1, b = '$'", "update t set a = n(1), b = s($)" },
				{ "delete mytable", null },
				{ "mytable", null },
				{ "history(x)", null },
				{ "(mytable)", "mytable" },
				{ "a sort b, c", null },
				{ "a sort reverse b, c", null },
		};
		for (String[] c : cases) {
			System.out.println(c[0]);
			assertEquals(c[1] == null ? c[0] : c[1], parse(c[0]));
		}
	}

	@Test(expected = SuException.class)
	public void bad() {
		parse("");
	}

	private String parse(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		StringQueryGenerator generator = new StringQueryGenerator();
		ParseQuery<String, QueryGenerator<String>> pc =
				new ParseQuery<String, QueryGenerator<String>>(lexer, generator);
		return pc.parse();
	}

}
