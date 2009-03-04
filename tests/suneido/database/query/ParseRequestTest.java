package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuException;
import suneido.language.Lexer;


public class ParseRequestTest {
	@Test
	public void test() {
		String[][] cases = new String[][] {
				{ "drop mytable", "drop(mytable)" },
				{ "Drop mytable", "drop(mytable)" },
				{ "rename one to two", "rename(one, two)" },
				{ "RENAME one TO two", "rename(one, two)" },
				{ "view myview = one join two", "view(myview, 'one join two')" },
				{ "sview myview = three", "sview(myview, 'three')" },
				{ "create a (b,c,d) key(b) index(c,d)", null },
				{ "ensure a (b,c,d) key(b) index(c,d)", null },
				{ "ensure a key(b)", null },
				{ "ensure a key(b) in c", null },
				{ "ensure a index(b) in c (d)", null },
				{ "ensure a index(b) in c cascade", null },
				{ "ensure a key(b) in c cascade update", null },
				{ "alter a create (b)", null },
				{ "alter a drop key(b)", null },
				{ "alter a rename b to c", null },
				{ "alter a rename b to c, d to e", null },
		};
		for (String[] c : cases) {
			System.out.println(c[0]);
			assertEquals(c[1] == null ? c[0] : c[1], parse(c[0]));
		}
	}

	@Test(expected = SuException.class)
	public void bad() {
		parse("create a key(b)");
	}

	private String parse(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		StringRequestGenerator generator = new StringRequestGenerator();
		ParseRequest<String> pc = new ParseRequest<String>(lexer, generator);
		return pc.parse();
	}

}
