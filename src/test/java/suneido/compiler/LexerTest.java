/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static suneido.compiler.Token.*;

import java.util.List;

import org.junit.Test;

import suneido.compiler.Lexer;
import suneido.compiler.Token;

import com.google.common.collect.Lists;

public class LexerTest {
	@Test
	public void empty() {
		check("");
	}

	@Test
	public void singleChar() {
		check("#(),:;?@[]{}.",
				HASH, L_PAREN, R_PAREN, COMMA, COLON, SEMICOLON, Q_MARK, AT,
				L_BRACKET, R_BRACKET, L_CURLY, R_CURLY, DOT);
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
	public void numberEnding() {
		check("100.Times", NUMBER, DOT, IDENTIFIER);
		check("#20090216.EndOfMonth", HASH, NUMBER, DOT, IDENTIFIER);
	}

	@Test
	public void rangeTo() {
		check("s[1 .. 2]", IDENTIFIER, L_BRACKET, NUMBER, RANGETO, NUMBER, R_BRACKET);
		check("x..y", IDENTIFIER, RANGETO, IDENTIFIER);
		check("x[1..]", IDENTIFIER, L_BRACKET, NUMBER, RANGETO, R_BRACKET);
		check("x[1 ..]", IDENTIFIER, L_BRACKET, NUMBER, RANGETO, R_BRACKET);
	}

	@Test
	public void operators() {
		check("= == != =~ !~ ! ++ -- < <= > >= << >> <<= >>= | |= & &= ^ ^=" +
				"+ += - -= $ $= * *= / /= % %= && || and or xor not is isnt",
				EQ, IS, ISNT, MATCH, MATCHNOT, NOT, INC, DEC,
				LT, LTE, GT, GTE, LSHIFT, RSHIFT, LSHIFTEQ, RSHIFTEQ,
				BITOR, BITOREQ, BITAND, BITANDEQ, BITXOR, BITXOREQ,
				ADD, ADDEQ, SUB, SUBEQ, CAT, CATEQ,
				MUL, MULEQ, DIV, DIVEQ, MOD, MODEQ, AND, OR,
				AND, OR, ISNT, NOT, IS, ISNT);
	}

	private static void check(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		List<Token> tokens = Lists.newArrayList();
		while (true) {
			Token token = lexer.next();
			if (token == EOF)
				break;
			tokens.add(token);
		}
		assertThat(tokens.toArray(new Token[0]), is(results));
	}

	@Test
	public void spanComment() {
		checkAll("@/* stuff \n */@", AT, COMMENT, AT);
		checkAll("if 1<2\n/**/Print(12)",
				IDENTIFIER, WHITE, NUMBER, LT, NUMBER, NEWLINE, COMMENT,
				IDENTIFIER, L_PAREN, NUMBER, R_PAREN);
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
		checkAll("\n ", NEWLINE);
	}

	private static void checkAll(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results)
			assertEquals(source, result, lexer.nextAll());
		assertEquals(source, EOF, lexer.nextAll());
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
				{ "'\\x0a'", "\n" },
				{ "`\\`", "\\" }
			};
			for (String[] s : cases)
				checkValue(s, STRING);
	}
	private static void checkValue(String[] source, Token... results) {
		Lexer lexer = new Lexer(source[0]);
		for (Token result : results) {
			assertEquals(source[0], result, lexer.next());
			assertEquals(source[0], source[1], lexer.getValue());
		}
		assertEquals(source[0], EOF, lexer.next());
	}

	@Test
	public void identifier() {
		String[] cases = new String[] {
				"a", "A", "_x", "abc_123", "Abc?", "abc!"
			};
			for (String s : cases)
				checkValue(s, IDENTIFIER);
	}
	private static void checkValue(String source, Token... results) {
		Lexer lexer = new Lexer(source);
		for (Token result : results) {
			assertEquals(source, result, lexer.next());
			assertEquals(source, lexer.getValue());
		}
		assertEquals(source, EOF, lexer.next());
	}

	@Test
	public void keywords() {
		checkKeywords("break case catch continue class callback default " +
			"dll do else for forever function if new " +
			"switch struct super return throw try while true false");
	}
	private static void checkKeywords(String source) {
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

	@Test
	public void lineNumber() {
		checkLine("", 1);
		checkLine("one two three", 1);
		checkLine(". one two three", 1);
		checkLine("one\ntwo\nthree", 3);
		checkLine("one\ntwo\nthree.\nfour", 3);
		checkLine("one '\n' two '\n' three", 3);
		checkLine("one /*\n*/ two /*\n*/ three", 3);
	}

	private static void checkLine(String source, int i) {
		Lexer lexer = new Lexer(source);
		int line = 0;
		while (true) {
			line = lexer.getLineNumber();
			Token token = lexer.next();
			if (token == DOT || token == EOF)
				break;
		}
		assertEquals(i, line);
	}

}
