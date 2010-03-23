package suneido.language;

import static suneido.language.Token.NEWLINE;
import suneido.SuException;

public class Parse<T, G> {

	protected final Lexer lexer;
	protected final G generator;
	public Token token = NEWLINE;
	protected int statementNest = 99;
	boolean expectingCompound = false;

	protected Parse(Lexer lexer, G generator) {
		this.lexer = lexer;
		this.generator = generator;
		match();
	}
	protected Parse(Parse<T, G> parse) {
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

	// PERF reuse a lookahead lexer (match calls this every time)
	protected Token lookAhead(boolean skipNewlines) {
		Lexer ahead = new Lexer(lexer);
		Token token = ahead.next();
		while (skipNewlines && token == NEWLINE)
			token = ahead.next();
		return token;
	}

}
