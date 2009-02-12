package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suneido.SuException;

public class ParseTest {
	@Test
	public void good() {
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
			"function () { }", "",
			"function () { x }", "x;",
			"function () { x; }", "x;",
			"function () { x ; y }", "x; y;",
			"function () { x; y; }", "x; y;",
			"function () { x \n y }", "x; y;",
			"function () { if (x) y }", "if (x) { y; }",
			"function () { if x y }", "if (x) { y; }",
			"function () { if (x) if (y) z }", "if (x) { if (y) { z; } }",
			"function () { if x if y z }", "if (x) { if (y) { z; } }",
			"function () { x; y; z; }", "x; y; z;",
			"function () { x; y; z }", "x; y; z;",
			"function () { x \n y \n z }", "x; y; z;",
			"function () { return }", "return;",
			"function () { return x }", "return x;",
			"function () { return \n x }", "return; x;",
			"function () { return; x }", "return; x;",
			"function () { if x return y }", "if (x) { return y; }",
			"function () { x ? y : a ? b : c }", "(x ? y : (a ? b : c));",
			"function () { x = y \n = z }", "x = y = z;",
		};
		for (int i = 0; i < cases.length; i += 2) {
			String s = cases[i];
System.out.println(s);
			String expect = cases[i + 1] == null ? s : cases[i + 1];
			assertEquals(s, expect, ParseLanguage.parse(s).toString());
		}
	}

	@Test
	public void bad() {
		String[] cases = {
				"function () { x y }",
				"function () { (x y) }",
				"function () { if a b if c d }",
			};
		for (int i = 0; i < cases.length; i += 2) {
			assertTrue(cases[i], badone(cases[i]));
		}
	}

	private boolean badone(String s) {
		try {
			ParseLanguage.parse(s);
			return false;
		} catch (SuException e) {
			return true;
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
