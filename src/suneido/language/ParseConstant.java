package suneido.language;

import static suneido.language.Token.NUMBER;
import static suneido.language.Token.STRING;
import suneido.SuException;

public class ParseConstant<T> extends Parse {
	private final Lexer lexer;
	private final Generator<T> generator;
	private Token token;
	private String value;

	ParseConstant(Lexer lexer, Generator<T> generator) {
		this.lexer = lexer;
		this.generator = generator;
		match();
	}

	public T constant() {
		switch (token) {
		case SUB:
			match();
			return matchReturn(NUMBER, generator.number("-" + value));
		case ADD:
			match();
			return number();
		case NUMBER:
			return number();
		case STRING:
			return string();
		case HASH:
			return hashConstant();
		case IDENTIFIER:
			return identifierConstant();
		}
		syntaxError();
		return null;
	}

	private T bool() {
		return matchReturn(generator.bool(value));
	}

	private T number() {
		return matchReturn(NUMBER, generator.number(value));
	}

	private T string() {
		return matchReturn(STRING, generator.string(value));
	}

	private T date() {
		return matchReturn(NUMBER, generator.date(value));
	}

	private T hashConstant() {
		match();
		switch (token) {
		case NUMBER:
			return date();
		case IDENTIFIER:
		case STRING:
			return symbol();
		}
		syntaxError();
		return null;
	}

	private T identifierConstant() {
		switch (lexer.getKeyword()) {
		case TRUE:
		case FALSE:
			return bool();
		}
		syntaxError();
		return null;
	}

	private T symbol() {
		return matchReturn(generator.symbol(value));
	}

	private T matchReturn(T result) {
		match();
		return result;
	}
	private T matchReturn(Token token, T result) {
		match(token);
		return result;
	}

	private void match() {
		token = lexer.next();
		value = lexer.getValue();
	}

	private void match(Token expected) {
		if (this.token != expected)
			syntaxError();
		match();
	}

	private void syntaxError() {
		throw new SuException("syntax error: unexpected " + token);
	}

	public void checkEof() {
		match(Token.EOF);
	}
}
