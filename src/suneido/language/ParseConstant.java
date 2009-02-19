package suneido.language;

import static suneido.language.Token.NUMBER;
import static suneido.language.Token.STRING;

public class ParseConstant<T> extends Parse<T> {
	ParseConstant(Lexer lexer, Generator<T> generator) {
		super(lexer, generator);
	}
	ParseConstant(Parse<T> parse) {
		super(parse);
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
}
