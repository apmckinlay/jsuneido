package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuException;

public class ParseTest {
	@Test
	public void test() {
		String[] cases = {
			"0", null,
			"123", null,
			"-3", null,
			"1e3", "1000",
			"1e-3", ".001",
			"0177", "127",
			"0xff", "255",
			"'hello'", null,
			"#20080101", null,
			"function () { }", "'FUNCTION'",
			"function () { x }", "'FUNCTION'",
			"function () { if (x) y }", "'FUNCTION'",
			"function () { if x y }", "'FUNCTION'",
			"function () { if (x) if (y) z }", "'FUNCTION'",
			"function () { if x if y z }", "'FUNCTION'",
			"function () { x; y; z; }", "'FUNCTION'",
			"function () { x y z }", "'FUNCTION'",
			"function () { return }", "'FUNCTION'",
			"function () { return x }", "'FUNCTION'",
			"function () { return; x }", "'FUNCTION'",
			"function () { if x return y }", "'FUNCTION'",
		};
		for (int i = 0; i < cases.length; i += 2) {
			String s = cases[i];
System.out.println(s);
			String expect = cases[i + 1] == null ? s : cases[i + 1];
			assertEquals(s, expect, ParseLanguage.parse(s).toString());
		}
	}

	@Test(expected = SuException.class)
	public void lexer_error() {
		ParseLanguage.parse("1e~3");
	}

	@Test(expected = SuException.class)
	public void extra_input() {
		ParseLanguage.parse("1 -5");
	}
}
