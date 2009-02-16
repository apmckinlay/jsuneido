package suneido.language;

import static org.junit.Assert.assertEquals;
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
				"+ += - -= $ $= * *= / /= % %= && ||",
				EQ, IS, ISNT, MATCH, MATCHNOT, NOT, INC, DEC,
				LT, LTE, GT, GTE, LSHIFT, RSHIFT, LSHIFTEQ, RSHIFTEQ,
				BITOR, BITOREQ, BITAND, BITANDEQ, BITXOR, BITXOREQ,
				ADD, ADDEQ, SUB, SUBEQ, CAT, CATEQ,
				MUL, MULEQ, DIV, DIVEQ, MOD, MODEQ, AND, OR);
	}

	@Test
	public void number() {
		String[] cases = new String[] {
			"0", "1", "01", "123", "0x0", "0x1f", "0x1F",
			"1e2", "1E-2", "123e+456", ".1", "1.1", "123.456", ".12e3"
		};
		for (String s : cases)
			check(s, NUMBER);
	}

	@Test
	public void string() {
		String[] cases = new String[] {
				"''", "\"\"", "'abc'", "\"abc\"",
				"'\"'", "\"'\"", "'\\''", "\"\\\"\"",
				"'\013'", "'\\x0d'"
			};
			for (String s : cases)
				check(s, STRING);
	}

	@Test
	public void identifier() {
		String[] cases = new String[] {
				"a", "A", "_x", "abc_123", "Abc?", "abc!"
			};
			for (String s : cases)
				check(s, IDENTIFIER);
	}

	private void check(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results)
			assertEquals(result, lexer.next());
		assertEquals(source, EOF, lexer.next());
	}
	private void checkAll(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results)
			assertEquals(result, lexer.nextAll());
		assertEquals(source, EOF, lexer.nextAll());
	}
}
