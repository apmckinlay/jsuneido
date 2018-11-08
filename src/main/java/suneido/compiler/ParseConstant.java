/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.Token.*;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import suneido.*;
import suneido.runtime.Numbers;
import suneido.runtime.Ops;

public class ParseConstant<T, G extends Generator<T>> extends Parse<T, G> {
	private static AtomicInteger classNum = new AtomicInteger();

	ParseConstant(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	public ParseConstant(Parse<T, G> parse) {
		super(parse);
	}

	public T parse(String name) {
		return matchReturn(EOF, constant(name));
	}

	public T constant() {
		return constant(null);
	}

	public T constant(String name) {
		return generator.value(constantValue(name));
	}

	// for simple values it returns the actual value
	// functions are still T (AstNode)
	public Object constantValue(String name) {
		switch (token) {
		case SUB:
			match();
			return Ops.uminus(number());
		case ADD:
			match();
			return Ops.uplus(number());
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
			if (lexer.getKeyword() != Token.NIL) {
				String val = lexer.getValue();
				match();
				return val;
			}
			break;
		case IDENTIFIER:
			switch (lexer.getKeyword()) {
			case FUNCTION:
				return function();
			case CLASS:
				return classConstant(name);
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
					return classConstant(name);
				match();
				return val;
			}
		}
		syntaxError();
		return null;
	}

	private boolean bool() {
		boolean val = lexer.getKeyword() == TRUE;
		match();
		return val;
	}

	private Number number() {
		String val = lexer.getValue();
		match(NUMBER);
		return Numbers.stringToNumber(val);
	}

	private String string() {
		// overhead of StringBuilder not worth it
		// because usually no concatenation
		String s = "";
		while (true) {
			s += lexer.getValue();
			match(STRING);
			if (token != CAT || lookAhead() != STRING)
				break;
			matchSkipNewlines(CAT);
		}
		return s;
	}

	private Object hashConstant() {
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

	private SuDate date() {
		SuDate date = SuDate.fromLiteral(lexer.getValue());
		if (date == null)
			throw new SuException("bad date literal");
		match(NUMBER);
		return date;
	}

	public T object() {
		int lineNumber = lexer.getLineNumber();
		var con = new ObjectContainer(
				token == L_PAREN ? new SuContainer() : new SuRecord());
		memberList(con, token, null);
		return generator.object(con.ob, lineNumber);
	}

	private T classConstant(String name) {
		if (name == null)
			name = "Class" + classNum.getAndIncrement();
		int lineNumber = lexer.getLineNumber();
		String base = classBase();
		var con = new ClassContainer();
		memberList(con, L_CURLY, name);
		return generator.clazz(name, base, con.data, lineNumber);
	}
	private String classBase() {
		if (lexer.getKeyword() == CLASS) {
			matchSkipNewlines(CLASS);
			matchIf(COLON);
		}
		if (token == L_CURLY)
			return null;
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

	private void memberList(Container con, Token opening, String className) {
		match(opening);
		var closing = opening.other;
		while (!matchIf(closing)) {
			member(con, closing, className);
			if (token == COMMA || token == SEMICOLON)
				match();
		}
	}
	private void member(Container con, Token closing, String className) {
		var inClass = className != null;
		Token start = token;
		var m = constantValue(null);
		if (matchIf(COLON)) {
			if (inClass)
				m = privatizeDef(className, m);
			if (token == COMMA || token == SEMICOLON || token == closing) {
				putMem(con, m, true);
			} else {
				var val = constantValue(null);
				putMem(con, m,  val);
			}
		} else if (inClass && start == IDENTIFIER && token == L_PAREN)
			putMem(con, privatizeDef(className, m), functionWithoutKeyword(inClass));
		else if (inClass)
			syntaxError("class members must be named");
		else
			con.add(m);
	}

	// privatize member names in class definitions
	private String privatizeDef(String className, Object m) {
		if (!(m instanceof String))
			syntaxError("class member names must be strings");
		String name = (String) m;
		if (name.startsWith("Getter_") &&
				name.length() > 7 && !Character.isUpperCase(name.charAt(7)))
			syntaxError("invalid getter (" + name + ")");
		if (!Character.isLowerCase(name.charAt(0)))
			return name;
		if (name.startsWith("getter_")) {
			if (name.length() <= 7 || !Character.isLowerCase(name.charAt(7)))
				syntaxError("invalid getter (" + name + ")");
			return "Getter_" + className + name.substring(6);
		}
		// TODO remove after transition from get_ to getter_
		if (name.startsWith("get_") && name.length() > 4
				&& Character.isLowerCase(name.charAt(4)))
			return "Get_" + className + name.substring(3);
		return className + "_" + name;
	}

	private void putMem(Container con, Object mem, Object val) {
		if (null != con.get(mem))
			syntaxError("duplicate member name (" + mem + ")");
		con.put(mem, val);
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

	private static abstract class Container {
		abstract void add(Object val);
		abstract Object get(Object key);
		abstract void put(Object key, Object val);
	}

	private static class ObjectContainer extends Container {
		final SuContainer ob; // could be SuRecord

		ObjectContainer(SuContainer ob) {
			this.ob = ob;
		}
		@Override
		void add(Object val) {
			ob.add(val);
		}
		@Override
		Object get(Object key) {
			return ob.getIfPresent(key);
		}
		@Override
		void put(Object key, Object val) {
			ob.put(key, val);
		}
	}

	private static class ClassContainer extends Container {
		final HashMap<String,Object> data = new HashMap<>();

		@Override
		void add(Object val) {
			throw SuInternalError.unreachable();
		}
		@Override
		Object get(Object key) {
			return data.get(key);
		}
		@Override
		void put(Object key, Object val) {
			data.put((String) key, val);
		}
	}

}
