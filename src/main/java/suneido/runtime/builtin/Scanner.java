/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.compiler.Token.EOF;

import java.util.Iterator;

import suneido.SuValue;
import suneido.compiler.Lexer;
import suneido.compiler.Token;
import suneido.runtime.*;

public class Scanner extends SuValue implements Iterable<String>, Iterator<String> {
	protected final Lexer lexer;
	private Token token;
	private static final BuiltinMethods methods = new BuiltinMethods(Scanner.class);

	public Scanner(String s) {
		lexer = new Lexer(s);
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static Object Next(Object self) {
		String s = ((Scanner) self).next();
		return s == null ? self : s;
	}

	public static Object Next2(Object self) {
		String s = ((Scanner) self).next2();
		return s == null ? self : s;
	}

	public static Object Position(Object self) {
		return ((Scanner) self).lexer.end();
	}

	public static Object Length(Object self) {
		return ((Scanner) self).lexer.length();
	}

	@Deprecated
	public static Object Type(Object self) {
		return ((Scanner) self).token.oldnum;
	}

	public static Object Type2(Object self) {
		return ((Scanner) self).type2();
	}

	public static Object Text(Object self) {
		return ((Scanner) self).lexer.matched();
	}

	public static Object Value(Object self) {
		return ((Scanner) self).lexer.getValue();
	}

	@Deprecated
	public static Object Keyword(Object self) {
		return ((Scanner) self).isKeyword() ? 1 : 0;
	}

	public static Object KeywordQ(Object self) {
		return ((Scanner) self).isKeyword();
	}

	protected boolean isKeyword() {
		Token keyword = lexer.getKeyword();
		return keyword != Token.NIL && keyword.ordinal() < Token.ALTER.ordinal();
	}

	public static Object Iter(Object self) {
		return self;
	}

	@Override
	public Iterator<String> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return lexer.hasNext();
	}

	@Override
	public String next() {
		token = lexer.nextAll();
		if (token == EOF)
			return null;
		return lexer.matched();
	}

	// no allocation, no use of oldnum
	public String next2() {
		token = lexer.nextAll();
		if (token == EOF)
			return null;
		return type2();
	}

	private String type2() {
		switch (token) {
		case ERROR:
			return "ERROR";
		case IDENTIFIER:
			return "IDENTIFIER";
		case NUMBER:
			return "NUMBER";
		case STRING:
			return "STRING";
		case WHITE:
			return "WHITESPACE";
		case COMMENT:
			return "COMMENT";
		case NEWLINE:
			return "NEWLINE";
		default:
			return "";
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static final BuiltinClass clazz = new BuiltinClass() {
		@Override
		public Scanner newInstance(Object... args) {
			args = Args.massage(FunctionSpec.string, args);
			return new Scanner(Ops.toStr(args[0]));
		}
	};

}
