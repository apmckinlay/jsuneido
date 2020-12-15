/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.Token.*;

import com.google.common.base.CharMatcher;

public class Lexer implements Doesc.Src {
	private final String source;
	private int si = 0;
	private int prev;
	private String value = "";
	private boolean valueIsSubstr;
	private Token keyword;
	private boolean ignoreCase = false;
	/** lineNum is the line number at lineNumPos */
	private int lineNum = 1;
	private int lineNumPos = 0;

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

	private static CharMatcher cm_nl = CharMatcher.is('\n');

	// Simpler to update on demand rather than track during lexing
	public int getLineNumber() {
		lineNum += cm_nl.countIn(srcsub(lineNumPos, prev));
		lineNumPos = prev;
		return lineNum;
	}

	public int getColumnNumber() {
		int prevLineEnd = prev - 1;
		for (; 0 <= prevLineEnd; --prevLineEnd) {
			final char c = source.charAt(prevLineEnd);
			if ('\n' == c || '\r' == c) {
				break;
			}
		}
		return prev - prevLineEnd;
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
		++si;
		switch (c) {
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
			return matchIf('=') ? BITOREQ : BITOR;
		case '&':
			return matchIf('=') ? BITANDEQ : BITAND;
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
		case ' ':
		case '\t':
		case '\n':
		case '\r':
			return whitespace(c);
		case '#':
			c = charAt(si);
			if (! (c == '_' || Character.isLetter(c)))
				return HASH;
			skipIdentifier();
			value = srcsub(prev + 1, si);
			valueIsSubstr = true;
			return STRING;
		case '_':
			return identifier();
		default:
			if (Character.isLetter(c))
				return identifier();
			else if (Character.isDigit(c))
				return number();
			else if (Character.isWhitespace(c))
				return whitespace(c);
			else
				return ERROR;
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
		value = srcsub(prev + 1, si - 1);
		valueIsSubstr = true;
		return STRING;
	}

	private Token quotedString(char quote) {
		char c;
		StringBuilder sb = new StringBuilder();
		for (; si < source.length() && (c = source.charAt(si)) != quote; ++si)
			if (c == '\\')
				sb.append(Doesc.doesc(this));
			else
				sb.append(c);
		if (si < source.length())
			++si;	// skip closing quote
		value = sb.toString();
		return STRING;
	}

	private Token identifier() {
		skipIdentifier();
		value(null);
		Token k = ignoreCase
				? Token.lookupIgnoreCase(value) : Token.lookup(value);
		if (charAt(si) == ':' && !(k == DEFAULT || k == TRUE || k == FALSE))
			return IDENTIFIER;
		keyword = k;
		return keyword != null && keyword.isOperator()
				? keyword : IDENTIFIER;
	}

	private void skipIdentifier() {
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
	}

	private Token number() {
		--si;
		if (hexNumber())
			return value(NUMBER);
		matchWhileDigit();
		if (matchIf('.'))
			matchWhileDigit();
		exponent();
		if (source.charAt(si - 1) == '.' && nonWhiteRemaining())
			--si; // don't absorb trailing period
		return value(NUMBER);
	}

	private boolean nonWhiteRemaining() {
		for (int i = si; i < source.length(); ++i)
			if (! Character.isWhitespace(source.charAt(i)))
				return true;
		return false;
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

	private Token whitespace(char c) {
		Token kind = WHITE;
		do {
			if ((c == '\n') || (c == '\r'))
				kind = NEWLINE;
			c = charAt(si);
			if (! Character.isWhitespace(c))
				break;
			++si;
		} while (true);
		return kind;
	}

	private Token value(Token token) {
		value = srcsub(prev, si);
		valueIsSubstr = true;
		return token;
	}

	private char charAt(int i) {
		return i < source.length() ? source.charAt(i) : 0;
	}

	public String remaining() {
		si = source.length();
		return srcsub(prev, si);
	}

	public int end() {
		return si;
	}

	public int length() {
		return si - prev;
	}

	public String matched() {
		return srcsub(prev, si);
	}

	private String srcsub(int from, int to) {
		int len = source.length();
		return (from >= len || to <= 0 || from >= to)
			? ""
			: source.substring(Math.max(0, from), Math.min(to, len));
	}

	// implement Src for Doesc

	@Override
	public char at(int d) {
		return charAt(si + d);
	}

	@Override
	public void move(int d) {
		si += d;
	}

//	public static void main(String[] args) {
//		Lexer lexer = new Lexer("function (.param, _param) { }");
//		while (lexer.hasNext()) {
//			Token token = lexer.next();
//			System.out.println(token);
//		}
//	}

}
