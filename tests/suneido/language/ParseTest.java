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

			"#20080101", null,
		};
		for (int i = 0; i < cases.length; i += 2) {
			String s = cases[i];
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
