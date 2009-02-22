package suneido.language;

import static suneido.language.Token.*;
import suneido.SuException;

public class Parse<T> {

	protected final Lexer lexer;
	protected final Generator<T> generator;
	protected Token token;
	protected String value;
	protected int statementNest = 0;

	Parse(Lexer lexer, Generator<T> generator) {
		this.lexer = lexer;
		this.generator = generator;
		match();
	}
	Parse(Parse<T> parse) {
		lexer = parse.lexer;
		generator = parse.generator;
		token = parse.token;
		value = parse.value;
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
		matchKeepNewline(token);
		if (statementNest > 0)
			while (token == NEWLINE)
				matchKeepNewline();
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
		value = lexer.getValue();
	}

	private void verifyMatch(Token expected) {
		if (this.token != expected && lexer.getKeyword() != expected)
			syntaxError("expected: " + expected + " got: " + token);
	}

	protected void syntaxError() {
		throw new SuException("syntax error: unexpected " + token);
	}
	protected void syntaxError(String s) {
		throw new SuException("syntax error: " + s);
	}

	public void checkEof() {
		match(Token.EOF);
	}

}
