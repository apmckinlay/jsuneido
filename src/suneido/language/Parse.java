/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.Token.NEWLINE;
import suneido.SuException;

public class Parse<T, G> {
	protected final Lexer lexer;
	protected final Lexer ahead;
	protected final G generator;
	public Token token = NEWLINE;
	protected int statementNest = 99;
	boolean expectingCompound = false;

	protected Parse(Lexer lexer, G generator) {
		this.lexer = lexer;
		ahead = new Lexer(lexer);
		this.generator = generator;
		match();
	}
	protected Parse(Parse<T, G> parse) {
		lexer = parse.lexer;
		ahead = parse.ahead;
		generator = parse.generator;
		token = parse.token;
		statementNest = parse.statementNest;
		expectingCompound = parse.expectingCompound;
	}

	protected T matchReturn(T result) {
		match();
		return result;
	}
	protected T matchReturn(Token expected, T result) {
		match(expected);
		return result;
	}

	protected boolean matchIf(Token possible) {
		if (token == possible || lexer.getKeyword() == possible) {
			match();
			return true;
		} else
			return false;
	}
	protected void match(Token expected) {
		verifyMatch(expected);
		match();
	}
	protected void match() {
		matchKeepNewline();
		if (statementNest != 0 || lookAhead().infix())
			while (token == NEWLINE)
				matchKeepNewline();
	}

	protected void matchSkipNewlines(Token token) {
		verifyMatch(token);
		matchSkipNewlines();
	}

	protected void matchSkipNewlines() {
		do
			matchKeepNewline();
		while (token == NEWLINE);
	}

	protected void matchKeepNewline(Token expected) {
		verifyMatch(expected);
		matchKeepNewline();
	}
	protected void matchKeepNewline() {
		switch (token) {
		case L_CURLY:
		case L_PAREN:
		case L_BRACKET:
			++statementNest;
			break;
		case R_CURLY:
		case R_PAREN:
		case R_BRACKET:
			--statementNest;
			break;
		default:
		}
		token = lexer.next();
		//System.out.println(token + " " + lexer.getValue());
	}

	protected void verifyMatch(Token expected) {
		if (this.token != expected && lexer.getKeyword() != expected)
			syntaxError("expected: " + expected + " got: " + token);
	}

	protected void syntaxError() {
		String value = lexer.getValue();
		syntaxError("unexpected " + token + (value == null ? "" : " " + value));
	}
	protected void syntaxError(String s) {
		throw new SuException("syntax error at line " + lexer.getLineNumber()
				+ ": " + s);
	}

	protected Token lookAhead() {
		return lookAhead(true);
	}

	protected Token lookAhead(boolean skipNewlines) {
		ahead.copyPosition(lexer);
		Token token = ahead.next();
		while (skipNewlines && token == NEWLINE)
			token = ahead.next();
		return token;
	}

	protected boolean anyName() {
		switch (token) {
		case IDENTIFIER:
		case STRING:
			return true;
		case IS:
		case ISNT:
		case AND:
		case OR:
		case NOT:
			String value = lexer.getValue();
			return Character.isLetter(value.charAt(0));
		default:
			return false;
		}
	}

}
