/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.Token.*;

import suneido.compiler.Generator.MType;

// TODO change to build and return the actual value, rather than AstNode tree
// inefficient to build AST and then traverse it to build e.g. object
// maybe handle line numbers in ParseFunction/Expression
// see cSuneido compile.cpp & gSuneido constant.go
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
		switch (token) {
		case SUB:
			match();
			return matchReturn(NUMBER, generator.number("-" + lexer.getValue(),
						lexer.getLineNumber()));
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
		case IS:
		case ISNT:
		case AND:
		case OR:
		case NOT:
			if (lexer.getKeyword() != Token.NIL)
				return matchReturn(generator.string(lexer.getValue(),
						lexer.getLineNumber()));
			break;
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
				String val = lexer.getValue();
				Token ahead = lookAhead();
				if (ahead != COLON && (val.equals("dll") ||
						val.equals("callback") || val.equals("struct")))
					syntaxError("jSuneido does not implement " + val);
				if (ahead == L_CURLY)
					return classConstant();
				return matchReturn(generator.string(val, lexer.getLineNumber()));
			}
		}
		syntaxError();
		return null;
	}

	private T classConstant() {
		int lineNumber = lexer.getLineNumber();
		String base = classBase();
		T members = memberList(L_CURLY, true);
		return generator.clazz(base == "" ? null : base, members, lineNumber);
	}
	private String classBase() {
		if (lexer.getKeyword() == CLASS) {
			matchSkipNewlines(CLASS);
			matchIf(COLON);
		}
		if (token == L_CURLY)
			return "";
		String base = lexer.getValue();
		if (!okBase(base))
			syntaxError("base class must be global defined in library");
		matchSkipNewlines(IDENTIFIER);
		return base;
	}
	private static boolean okBase(String s) {
		int i = s.startsWith("_") ? 1 : 0;
		return s.length() > i && Character.isUpperCase(s.charAt(i));
	}

	private T memberList(Token open, boolean inClass) {
		MType which = (open == L_PAREN) ? MType.OBJECT : MType.RECORD;
		match(open);
		T members = null;
		while (token != open.other) {
			members = generator.memberList(which, members, member(open.other, inClass));
			if (token == COMMA || token == SEMICOLON)
				match();
		}
		match(open.other);
		return members;
	}
	private T member(Token closing, boolean inClass) {
		Token start = token;
		T name = null;
		T value = constant();
		if (inClass && start == IDENTIFIER && token == L_PAREN) {
			name = value;
			value = functionWithoutKeyword(inClass);
		} else if (matchIf(COLON)) {
			name = value;
			if (token == COMMA || token == SEMICOLON || token == closing) {
				value = generator.boolTrue(lexer.getLineNumber());
			} else {
				value = constant();
			}
		}
		if (inClass && name == null)
			syntaxError("class members must be named");
		return generator.memberDefinition(name, value);
	}

	private T bool() {
		return matchReturn(generator.bool(lexer.getKeyword() == TRUE,
				lexer.getLineNumber()));
	}

	private T number() {
		return matchReturn(NUMBER,
				generator.number(lexer.getValue(), lexer.getLineNumber()));
	}

	private T string() {
		String s = "";
		while (true) {
			s += lexer.getValue();
			match(STRING);
			if (token != CAT || lookAhead() != STRING)
				break;
			matchSkipNewlines(CAT);
		}
		return generator.string(s, lexer.getLineNumber());
	}

	private T date() {
		T date = generator.date(lexer.getValue(), lexer.getLineNumber());
		if (date == null)
			syntaxError("invalid date literal: " + lexer.getValue());
		return matchReturn(NUMBER, date);
	}

	private T hashConstant() {
		match();
		switch (token) {
		case NUMBER:
			return date();
		case L_PAREN:
		case L_CURLY:
		case L_BRACKET:
			return object();
		default:
			syntaxError();
			return null;
		}
	}

	public T object() {
		int lineNumber = lexer.getLineNumber();
		MType which = (token == L_PAREN ? MType.OBJECT : MType.RECORD);
		T members = memberList(token, false);
		return generator.object(which, members, lineNumber);
	}

	private T function() {
		matchSkipNewlines(FUNCTION);
		return functionWithoutKeyword(false);
	}

	private T functionWithoutKeyword(boolean inClass) {
		ParseFunction<T, G> p = new ParseFunction<>(this);
		T result = p.functionWithoutKeyword(inClass);
		token = p.token;
		return result;
	}

}
