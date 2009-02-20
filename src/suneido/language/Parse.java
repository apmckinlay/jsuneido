package suneido.language;

import suneido.SuException;
import static suneido.language.Token.*;

public class Parse<T> {

	protected final Lexer lexer;
	protected final Generator<T> generator;
	protected Token token;
	protected String value;

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

	protected T matchReturn(Token token, T result) {
		match(token);
		return result;
	}

	protected void match() {
		token = lexer.next();
		value = lexer.getValue();
	}

	protected void match(Token expected) {
		if (this.token == expected ||
				(this.token == IDENTIFIER && lexer.getKeyword() == expected))
			match();
		else
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
