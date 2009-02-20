package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ParseConstantTest {
	@Test
	public void test() {
		String[][] cases = new String[][] {
			{ "123", "n(123)" },
			{ "+123", "n(123)" },
			{ "-123", "n(-123)" },
			{ "'abc'", "s(abc)" },
			{ "#20090219", "d(20090219)" },
			{ "#foo", "sym(foo)" },
			{ "#'foo bar'", "sym(foo bar)" },
			{ "true", "true" },
			{ "false", "false" },
		};
		for (String[] c : cases) {
System.out.println(c[0]);
			assertEquals(c[1], parse(c[0]));
		}
	}

	private String parse(String s) {
		Lexer lexer = new Lexer(s);
		StringGenerator generator = new StringGenerator();
		ParseConstant<String> pc = new ParseConstant<String>(lexer, generator);
		String result = pc.constant();
		pc.checkEof();
		return result;
	}
}
