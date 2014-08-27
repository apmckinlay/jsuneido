/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.Token.*;

public class Lexer {
	private final String source;
	private int si = 0;
	private int prev;
	private String value = "";
	private boolean valueIsSubstr;
	private Token keyword;
	private boolean ignoreCase = false;
	private int lineNumber = 1;

	public Lexer(String source) {
		this.source = source;
	}

	public Lexer(Lexer lexer) {
		source = lexer.source;
		copyPosition(lexer);
	}

	public void copyPosition(Lexer lexer) {
		assert source == lexer.source;
		si = lexer.si;
	}

	public void ignoreCase() {
		ignoreCase = true;
	}

	public String getValue() {
		// use copy constructor if value is substr of source 
		// to avoid reference to source
		return valueIsSubstr ? new String(value) : value;
	}

	public Token getKeyword() {
		return keyword == null ? NIL : keyword;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getColumnNumber() {
		int prevLineEnd = prev - 1;
		for (; 0 <= prevLineEnd; --prevLineEnd) {
			final char c = source.charAt(prevLineEnd);
			if ('\n' == c || '\r' == c) {
				break;
			}
		}
		final int lineStart = prevLineEnd + 1;
		return prev - lineStart;
	}

	public Token nextSkipNewlines() {
		Token token;
		do
			token = next();
		while (token == NEWLINE);
		return token;
	}

	public Token next() {
		Token token;
		do
			token = nextAll();
			while (token == WHITE || token == COMMENT);
		return token;
	}

	public boolean hasNext() {
		return si < source.length();
	}

	public Token nextAll() {
		value = "";
		valueIsSubstr = false;
		keyword = null;
		prev = si;
		if (si >= source.length())
			return EOF;
		char c = source.charAt(si);
		if (Character.isWhitespace(c))
			return whitespace();
		++si;
		switch (c) {
		case '#':
			return HASH;
		case '(':
			return L_PAREN;
		case ')':
			return R_PAREN;
		case ',':
			return COMMA;
		case ';':
			return SEMICOLON;
		case '?':
			return Q_MARK;
		case '@':
			return AT;
		case '[':
			return L_BRACKET;
		case ']':
			return R_BRACKET;
		case '{':
			return L_CURLY;
		case '}':
			return R_CURLY;
		case '~':
			return BITNOT;
		case ':':
			return matchIf(':') ? RANGELEN : COLON;
		case '=' :
			return matchIf('=') ? IS : matchIf('~') ? MATCH : EQ;
		case '!':
			return matchIf('=') ? ISNT : matchIf('~') ? MATCHNOT : NOT;
		case '<':
			return matchIf('<') ? (matchIf('=') ? LSHIFTEQ : LSHIFT)
					: matchIf('>') ? ISNT : matchIf('=') ? LTE : LT;
		case '>':
			return matchIf('>') ? (matchIf('=') ? RSHIFTEQ : RSHIFT)
					: matchIf('=') ? GTE : GT;
		case '|':
			return matchIf('|') ? OR : matchIf('=') ? BITOREQ : BITOR;
		case '&':
			return matchIf('&') ? AND : matchIf('=') ? BITANDEQ : BITAND;
		case '^':
			return matchIf('=') ? BITXOREQ : BITXOR;
		case '-':
			return matchIf('-') ? DEC : matchIf('=') ? SUBEQ : SUB;
		case '+':
			return matchIf('+') ? INC : matchIf('=') ? ADDEQ : ADD;
		case '/':
			return matchIf('/') ? lineComment() : matchIf('*') ? spanComment()
					: matchIf('=') ? DIVEQ : DIV;
		case '*':
			return matchIf('=') ? MULEQ : MUL;
		case '%':
			return matchIf('=') ? MODEQ : MOD;
		case '$':
			return matchIf('=') ? CATEQ : CAT;
		case '`':
			return rawString();
		case '"':
		case '\'':
			return quotedString(c);
		case '.':
			return matchIf('.') ? RANGETO
					: Character.isDigit(charAt(si)) ? number()
					: DOT;
		default:
			return Character.isDigit(c) ? number()
					: (c == '_' || Character.isLetter(c)) ? identifier()
					: ERROR;
		}
	}

	private boolean matchIf(char c) {
		if (si < source.length() && source.charAt(si) == c) {
			++si;
			return true;
		} else
			return false;
	}

	private Token spanComment() {
		while (si < source.length() && (charAt(si) != '*' || charAt(si + 1) != '/'))
			++si;
		if (si < source.length())
			si += 2;
		return COMMENT;
	}

	private Token lineComment() {
		for (++si; si < source.length() &&
			'\r' != charAt(si) && '\n' != charAt(si); ++si)
			;
		return COMMENT;
	}

	// backquoted strings not escaped, can contain anything except backquotes
	private Token rawString() {
		while (si < source.length() && source.charAt(si) != '`')
			++si;
		if (si < source.length())
			++si;	// skip closing quote
		value = source.substring(prev + 1, si - 1);
		valueIsSubstr = true;
		return STRING;
	}

	private Token quotedString(char quote) {
		char c;
		StringBuilder sb = new StringBuilder();
		for (; si < source.length() && (c = source.charAt(si)) != quote; ++si)
			if (c == '\\')
				sb.append(doesc());
			else
				sb.append(c);
		if (si < source.length())
			++si;	// skip closing quote
		value = sb.toString();
		return STRING;
	}

	private Token identifier() {
		char c;
		while (true) {
			c = charAt(si);
			if (Character.isLetterOrDigit(c) || c == '_')
				++si;
			else
				break;
		}
		if (c == '?' || c == '!')
			++si;
		value(null);
		keyword = ignoreCase
				? Token.lookupIgnoreCase(value) : Token.lookup(value);
		if (charAt(si) == ':' &&
				(keyword == Token.IS || keyword == Token.ISNT ||
				keyword == Token.AND || keyword == Token.OR || keyword == Token.NOT))
			keyword = null;
		return keyword != null && keyword.isOperator()
				? keyword : IDENTIFIER;
	}

	private Token number() {
		--si;
		if (hexNumber())
			return value(NUMBER);
		matchWhileDigit();
		if (matchIf('.'))
			matchWhileDigit();
		exponent();
		if (source.charAt(si - 1) == '.')
			--si; // don't absorb trailing period
		return value(NUMBER);
	}

	private boolean hexNumber() {
		if (matchIf('0') && (matchIf('x') || matchIf('X'))) {
			while (-1 != Character.digit(charAt(si), 16))
				++si;
			// NOTE: this accepts "0x"
			return true;
		}
		return false;
	}

	private boolean matchWhileDigit() {
		int start = si;
		while (si < source.length() && Character.isDigit(source.charAt(si)))
			++si;
		return si > start;
	}

	private void exponent() {
		int save = si;
		if (! matchIf('e') && ! matchIf('E'))
			return;
		if (! matchIf('+'))
			matchIf('-');
		if (! matchWhileDigit())
			si = save;
	}

	private Token whitespace() {
		char c;
		boolean eol = false;
		for (; Character.isWhitespace(c = charAt(si)); ++si)
			if (c == '\n') {
				eol = true;
				++lineNumber;
			} else if (c == '\r')
				eol = true;
		return eol ? NEWLINE : WHITE;
	}

	private Token value(Token token) {
		value = source.substring(prev, si);
		valueIsSubstr = true;
		return token;
	}

	private char doesc() {
		++si; // backslash
		int dig1, dig2, dig3;
		switch (charAt(si)) {
		case 'n' :
			return '\n';
		case 't' :
			return '\t';
		case 'r' :
			return '\r';
		case 'x' :
			if (-1 != (dig1 = Character.digit(charAt(si + 1), 16)) &&
					-1 != (dig2 = Character.digit(charAt(si + 2), 16))) {
				si += 2;
				return (char) (16 * dig1 + dig2);
			} else
				return source.charAt(--si);
		case '\\' :
		case '"' :
		case '\'' :
			return charAt(si);
		default :
			if (-1 != (dig1 = Character.digit(charAt(si), 8)) &&
					-1 != (dig2 = Character.digit(charAt(si + 1), 8)) &&
					-1 != (dig3 = Character.digit(charAt(si + 2), 8))) {
				si += 2;
				return (char) (64 * dig1 + 8 * dig2 + dig3);
			} else
				return source.charAt(--si);
		}
	}

	private char charAt(int i) {
		return i < source.length() ? source.charAt(i) : 0;
	}

	public String remaining() {
		si = source.length();
		return source.substring(prev);
	}

	public int end() {
		return si;
	}

	public int length() {
		return si - prev;
	}

	public String matched() {
		return source.substring(prev, si);
	}

	public static void main(String[] args) {
		Lexer lexer = new Lexer("function (.param, _param) { }");
		while (lexer.hasNext()) {
			Token token = lexer.next();
			System.out.println(token);
		}
	}

}
