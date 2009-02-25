package suneido.language;

import static suneido.language.Token.*;
import static suneido.language.Generator.ObjectOrRecord.*;
import suneido.language.Generator.ObjectOrRecord;

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
			return matchReturn(NUMBER, generator.number("-" + lexer.getValue()));
		case ADD:
			match();
			return number();
		case NUMBER:
			return number();
		case STRING:
			return string();
		case HASH:
			return hashConstant();
		case L_PAREN:
		case L_CURLY:
		case L_BRACKET:
			return object();
		case IDENTIFIER:
			switch (lexer.getKeyword()) {
			case FUNCTION:
				return function();
			case CLASS:
				return classConstant();
			case TRUE:
			case FALSE:
				return bool();
			default:
				if (lookAhead() == L_CURLY)
					return classConstant();
				else
					return matchReturn(IDENTIFIER, generator.string(lexer.getValue()));
			}
		}
		syntaxError();
		return null;
	}

	private T function() {
		ParseFunction<T> p = new ParseFunction<T>(this);
		T result = p.function();
		token = p.token;
		return result;
	}

	private T classConstant() {
		String base = classBase();
		T members = memberList(L_CURLY);
		return generator.classConstant(base, members);
	}
	private String classBase() {
		if (matchIf(CLASS) && ! matchIf(COLON))
			return null;
		String base = lexer.getValue();
		match(IDENTIFIER);
		return base;
	}
	private T memberList(Token open) {
		match(open);
		T members = null;
		while (token != open.other) {
			members = generator.memberList(members, member());
			if (token == COMMA || token == SEMICOLON)
				match();
		}
		match(open.other);
		return members;
	}
	private T member() {
		T name = memberName();
		T value = memberValue(name);
		return generator.memberDefinition(name, value);
	}
	private T memberName() {
		if (! isMemberName())
			return null;
		T name = null;
		if (token == IDENTIFIER || token == STRING
				|| token == NUMBER || token == SUB || token == ADD) {
			name = constant();
		} else
			syntaxError();
		if (token == COLON)
			match();
		else if (token != L_PAREN)
			syntaxError();
		return name;
	}
	private boolean isMemberName() {
		Lexer ahead = new Lexer(lexer);
		Token next = ahead.next();
		if (token == SUB || token == ADD)
			next = ahead.next();
		return next == COLON || next == L_PAREN;
	}

	private T memberValue(T name) {
		T value = null;
		if (token == L_PAREN)
			value = functionWithoutKeyword();
		else if (token != COMMA && token != R_PAREN && token != R_CURLY) {
			value = constant();
		} else if (name != null)
			value = generator.bool("true");
		else
			syntaxError();
		return value;
	}
	private T functionWithoutKeyword() {
		ParseFunction<T> p = new ParseFunction<T>(this);
		T result = p.functionWithoutKeyword();
		token = p.token;
		return result;
	}

	private T bool() {
		return matchReturn(generator.bool(lexer.getValue()));
	}

	private T number() {
		return matchReturn(NUMBER, generator.number(lexer.getValue()));
	}

	private T string() {
		return matchReturn(STRING, generator.string(lexer.getValue()));
	}

	private T date() {
		return matchReturn(NUMBER, generator.date(lexer.getValue()));
	}

	private T hashConstant() {
		match();
		switch (token) {
		case NUMBER:
			return date();
		case IDENTIFIER:
		case STRING:
			return symbol();
		case L_PAREN:
		case L_CURLY:
		case L_BRACKET:
			return object();
		}
		syntaxError();
		return null;
	}

	private T object() {
		ObjectOrRecord which = token == L_PAREN ? OBJECT : RECORD;
		T members = memberList(token);
		return generator.object(which, members);
	}

	private T symbol() {
		return matchReturn(generator.symbol(lexer.getValue()));
	}
}
