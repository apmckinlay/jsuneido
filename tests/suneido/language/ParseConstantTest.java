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
			{ "true", "b(true)" },
			{ "false", "b(false)" },
			{ "Global", "s(Global)" },
			{ "function () { }", "function () { }" },
			{ "function\n () { }", "function () { }" },
			{ "function ()\n { }", "function () { }" },
			{ "function (@args) { }", "function (@args) { }" },
			{ "function (a, b, c = 1, d = 2) { }",
				"function (a, b, c = n(1), d = n(2)) { }" },
			{ "class { }", "class { }" },
			{ "class\n { }", "class { }" },
			{ "class : Base { }", "class : Base { }" },
			{ "Base { }", "class : Base { }" },
			{ "Base { a: }", "class : Base { s(a): b(true) }" },
			{ "Base { 12: 34 }", "class : Base { n(12): n(34) }" },
			{ "Base { -12: 'abc' }", "class : Base { n(-12): s(abc) }" },
			{ "class { a: 1; b: 2, c: 3 \n d: 4}",
				"class { s(a): n(1), s(b): n(2), s(c): n(3), s(d): n(4) }" },
			{ "class { f() { x } }", "class { s(f): function () { x; } }" },
			{ "#()", "#()" },
			{ "#{}", "#{}" },
			{ "#(1, 'a', b: 2)", "#(n(1), s(a), s(b): n(2))" },
			{ "#({})", "#(#{})" },
			{ "#(class: 123)", "#(s(class): n(123))" },
			{ "class { one\n two }", "class { s(one), s(two) }" },
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
