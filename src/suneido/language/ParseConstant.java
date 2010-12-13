package suneido.language;

import static suneido.language.Token.*;
import suneido.SuException;
import suneido.language.Generator.MType;

public class ParseConstant<T, G extends Generator<T>> extends Parse<T, G> {

	ParseConstant(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	public ParseConstant(Parse<T, G> parse) {
		super(parse);
	}

	public T parse() {
		return matchReturn(EOF, constant());
	}

	public T constant() {
		if (token == IDENTIFIER)
			switch (lexer.getKeyword()) {
			case FUNCTION:
				return function();
			case CLASS:
				return classConstant();
			case DLL:
			case STRUCT:
			case CALLBACK:
				throw new SuException("jSuneido does not support "
						+ lexer.getValue() + " line " + lexer.getLineNumber());
			case TRUE:
			case FALSE:
				return bool();
			default:
				if (lookAhead() == L_CURLY)
					return classConstant();
			}
		return simpleConstant();
	}

	public T simpleConstant() {
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
			return matchReturn(IDENTIFIER, generator.string(lexer.getValue()));
		}
		syntaxError();
		return null;
	}

	private T classConstant() {
		String base = classBase();
		T members = memberList(L_CURLY, true);
		return generator.clazz(base == "" ? null : base, members);
	}
	private String classBase() {
		if (lexer.getKeyword() == CLASS) {
			matchSkipNewlines(CLASS);
			if (!matchIf(COLON))
				return "";
		}
		String base = lexer.getValue();
		int i = base.startsWith("_") ? 1 : 0;
		if (!Character.isUpperCase(base.charAt(i)))
			syntaxError("base class must be global defined in library");
		matchSkipNewlines(IDENTIFIER);
		return base;
	}
	private T memberList(Token open, boolean inClass) {
		MType which = (open == L_PAREN) ? MType.OBJECT : MType.RECORD;
		match(open);
		T members = null;
		while (token != open.other) {
			members = generator.memberList(which, members, member(inClass));
			if (token == COMMA || token == SEMICOLON)
				match();
		}
		match(open.other);
		return members;
	}
	private T member(boolean inClass) {
		T name = memberName(inClass);
		boolean canBeFunction = token != COLON;
		if (name != null) {
			if (token == COLON) {
				match();
				if (inClass && token == IDENTIFIER && lexer.getKeyword() == FUNCTION) {
					canBeFunction = true;
					match();
				}
			} else if (inClass && token != L_PAREN)
				syntaxError("expected colon after member name");
		}
		T value = memberValue(name, canBeFunction, inClass);
		return generator.memberDefinition(name, value);
	}
	private T memberName(boolean inClass) {
		if (!isMemberName(inClass))
			return null;
		T name = simpleConstant();
		return name;
	}
	private boolean isMemberName(boolean inClass) {
		if (token != IDENTIFIER && token != STRING && token != NUMBER
				&& token != SUB && token != ADD)
			return false;
		Lexer ahead = new Lexer(lexer);
		Token next = ahead.next();
		if (token == SUB || token == ADD)
			next = ahead.next();
		return next == COLON || (inClass && next == L_PAREN);
	}
	private T memberValue(T name, boolean canBeFunction, boolean inClass) {
		T value = null;
		if (name != null && canBeFunction && token == L_PAREN)
			value = functionWithoutKeyword(inClass);
		else if (token != COMMA && token != R_PAREN && token != R_CURLY) {
			if (token == FUNCTION) {
				matchSkipNewlines(FUNCTION);
				value = functionWithoutKeyword(inClass);
			} else if (token == CLASS)
				value = classConstant();
			else
				value = constant();
		} else if (name != null)
			value = generator.bool(true);
		else
			syntaxError();
		return value;
	}

	private T bool() {
		return matchReturn(generator.bool(lexer.getKeyword() == TRUE));
	}

	private T number() {
		return matchReturn(NUMBER, generator.number(lexer.getValue()));
	}

	private T string() {
		return matchReturn(STRING, generator.string(lexer.getValue()));
	}

	private T date() {
		T date = generator.date(lexer.getValue());
		if (date == null)
			syntaxError("invalid date literal: " + lexer.getValue());
		return matchReturn(NUMBER, date);
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

	private T symbol() {
		return matchReturn(generator.symbol(lexer.getValue()));
	}

	public T object() {
		MType which = (token == L_PAREN ? MType.OBJECT : MType.RECORD);
		T members = memberList(token, false);
		return generator.object(which, members);
	}

	private T function() {
		matchSkipNewlines(FUNCTION);
		return functionWithoutKeyword(false);
	}

	private T functionWithoutKeyword(boolean inClass) {
		ParseFunction<T, G> p = new ParseFunction<T, G>(this);
		T result = p.functionWithoutKeyword(inClass);
		token = p.token;
		return result;
	}
}
