package suneido.language;

import static suneido.language.Token.*;
import suneido.SuException;

public class Parse<T, Gen> {

	protected final Lexer lexer;
	protected final Gen generator;
	protected Token token;
	protected int statementNest = 99;
	boolean expectingCompound = false;

	protected Parse(Lexer lexer, Gen generator) {
		this.lexer = lexer;
		this.generator = generator;
		match();
	}
	protected Parse(Parse<T, Gen> parse) {
		lexer = parse.lexer;
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
		if (token == L_CURLY || token == L_PAREN || token == L_BRACKET)
			++statementNest;
		if (token == R_CURLY || token == R_PAREN || token == R_BRACKET)
			--statementNest;
		token = lexer.next();
	}

	private void verifyMatch(Token expected) {
		if (this.token != expected && lexer.getKeyword() != expected)
			syntaxError("expected: " + expected + " got: " + token);
	}

	protected void syntaxError() {
		String value = lexer.getValue();
		throw new SuException("syntax error: unexpected " + token +
				(value == null ? "" : " " + value));
	}
	protected void syntaxError(String s) {
		throw new SuException("syntax error: " + s);
	}

	public void checkEof() {
		match(Token.EOF);
	}
	protected Token lookAhead() {
		return lookAhead(true);
	}

	protected Token lookAhead(boolean skipNewlines) {
		Lexer ahead = new Lexer(lexer);
		Token token = ahead.next();
		while (skipNewlines && token == NEWLINE)
			token = ahead.next();
		return token;
	}

}
