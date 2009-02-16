package suneido.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static suneido.language.Token.*;

import org.junit.Test;

public class LexerTest {
	@Test
	public void empty() {
		check("");
	}

	@Test
	public void singleChar() {
		check("#(),:;?@[]{}",
				HASH, L_PAREN, R_PAREN, COMMA, COLON, SEMICOLON, Q_MARK, AT,
				L_BRACKET, R_BRACKET, L_CURLY, R_CURLY);
	}

	@Test
	public void ignoreWhite() {
		check(" \t");
	}

	@Test
	public void ignoreComment() {
		check("@ /* stuff \n */ @", AT, AT);
	}

	@Test
	public void spanComment() {
		checkAll("@/* stuff \n */@", AT, COMMENT, AT);
	}

	@Test
	public void lineComment() {
		checkAll("@// more\n@", AT, COMMENT, NEWLINE, AT);
	}

	@Test
	public void white() {
		checkAll(" \t", WHITE);
	}

	@Test
	public void newline() {
		checkAll(" \n ", NEWLINE);
	}

	@Test
	public void operators() {
		check("= == != =~ !~ ! ++ -- < <= > >= << >> <<= >>= | |= & &= ^ ^=" +
				"+ += - -= $ $= * *= / /= % %= && || and or not is isnt xor",
				EQ, IS, ISNT, MATCH, MATCHNOT, NOT, INC, DEC,
				LT, LTE, GT, GTE, LSHIFT, RSHIFT, LSHIFTEQ, RSHIFTEQ,
				BITOR, BITOREQ, BITAND, BITANDEQ, BITXOR, BITXOREQ,
				ADD, ADDEQ, SUB, SUBEQ, CAT, CATEQ,
				MUL, MULEQ, DIV, DIVEQ, MOD, MODEQ, AND, OR,
				AND, OR, NOT, IS, ISNT, ISNT);
	}

	@Test
	public void number() {
		String[] cases = new String[] {
			"0", "1", "01", "123", "0x0", "0x1f", "0x1F",
			"1e2", "1E-2", "123e+456", ".1", "1.1", "123.456", ".12e3"
		};
		for (String s : cases)
			checkValue(s, NUMBER);
	}

	@Test
	public void string() {
		String[][] cases = new String[][] {
				{ "''", "" },
				{ "\"\"", "" },
				{ "'abc'", "abc" },
				{ "\"abc\"", "abc" },
				{ "'\"'", "\"" },
				{ "\"'\"", "'" },
				{ "'\\''", "'" },
				{ "\"\\\"\"", "\"" },
				{ "'\\n'", "\n" },
				{ "'\\r'", "\r" },
				{ "'\\t'", "\t" },
				{ "'\\015'", "\r" },
				{ "'\\x0a'", "\n" }
			};
			for (String[] s : cases)
				checkValue(s, STRING);
	}

	@Test
	public void identifier() {
		String[] cases = new String[] {
				"a", "A", "_x", "abc_123", "Abc?", "abc!"
			};
			for (String s : cases)
				checkValue(s, IDENTIFIER);
	}

	@Test
	public void keywords() {
		checkKeywords("break case catch continue class callback default " +
			"dll do else for foreach forever function if " +
			"new switch struct super return throw try while true false");
	}

	private void check(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results)
			assertEquals(result, lexer.next());
		assertEquals(source, EOF, lexer.next());
	}
	private void checkValue(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results) {
			assertEquals(source, result, lexer.next());
			assertEquals(source, lexer.getValue());
		}
		assertEquals(source, EOF, lexer.next());
	}
	private void checkKeywords(String source) {
		Lexer lexer = new Lexer(source);
		Token token;
		while (EOF != (token = lexer.next())) {
			assertEquals(IDENTIFIER, token);
			assertNotNull(lexer.getValue(), lexer.getKeyword());
			assertEquals(lexer.getValue(),
					lexer.getKeyword().toString().toLowerCase());
		}
		assertEquals(source, EOF, lexer.next());
	}
	private void checkValue(String[] source, Token... results) {
		Lexer lexer = new Lexer(source[0]);
		for (Token result : results) {
			assertEquals(source[0], result, lexer.next());
			assertEquals(source[0], source[1], lexer.getValue());
		}
		assertEquals(source[0], EOF, lexer.next());
	}
	private void checkAll(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results)
			assertEquals(source, result, lexer.nextAll());
		assertEquals(source, EOF, lexer.nextAll());
	}
}
