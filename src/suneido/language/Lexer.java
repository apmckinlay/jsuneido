package suneido.language;

import static suneido.language.Token.*;

public class Lexer {
	private final String source;
	private int si = 0;
	private int prev;
	private String value = "";
	private Token keyword;
	private boolean ignoreCase = false;
	//private final String debug = ">";

	public Lexer(String source) {
		this.source = source;
	}
	public Lexer(Lexer lexer) {
		source = lexer.source;
		si = lexer.si;
		prev = lexer.prev;
		value = lexer.value;
		keyword = lexer.keyword;
		//debug = ">>";
	}

	public void ignoreCase() {
		ignoreCase = true;
	}

	public String getValue() {
		return value;
	}

	public Token getKeyword() {
		return keyword == null ? NIL : keyword;
	}

	public Token next() {
		Token token;
		do
			token = nextAll();
			while (token == WHITE || token == COMMENT);
		//System.out.println(debug + " " + token + (value == null ? "" : " " + value));
		return token;
	}

	public Token nextAll() {
		value = null;
		keyword = null;
		prev = si;
		if (si >= source.length())
			return EOF;
		char c = source.charAt(si);
		if (Character.isWhitespace(c))
			return whitespace();
		++si;
		switch (c) {
		case '#': return HASH;
		case '(': return L_PAREN;
		case ')': return R_PAREN;
		case ',': return COMMA;
		case ':': return COLON;
		case ';': return SEMICOLON;
		case '?': return Q_MARK;
		case '@': return AT;
		case '[': return L_BRACKET;
		case ']': return R_BRACKET;
		case '{': return L_CURLY;
		case '}': return R_CURLY;
		case '~': return BITNOT;
		case '=' :
			switch (charAt(si)) {
			case '=' : ++si; return IS;
			case '~' : ++si; return MATCH;
			default:
				return EQ;
			}
		case '!':
			switch (charAt(si)) {
			case '=':
				++si;
				return ISNT;
			case '~':
				++si;
				return MATCHNOT;
			default:
				return NOT;
			}
		case '<':
			switch (charAt(si)) {
			case '<':
				if (charAt(++si) == '=') {
					++si;
					return LSHIFTEQ;
				} else
					return LSHIFT;
			case '>':
				++si;
				return ISNT;
			case '=':
				++si;
				return LTE;
			default:
				return LT;
			}
		case '>':
			switch (charAt(si)) {
			case '>':
				if (charAt(++si) == '=') {
					++si;
					return RSHIFTEQ;
				} else
					return RSHIFT;
			case '=':
				++si;
				return GTE;
			default:
				return GT;
			}
		case '|':
			switch (charAt(si)) {
			case '|':
				++si;
				return OR;
			case '=':
				++si;
				return BITOREQ;
			default:
				return BITOR;
			}
		case '&':
			switch (charAt(si)) {
			case '&':
				++si;
				return AND;
			case '=':
				++si;
				return BITANDEQ;
			default:
				return BITAND;
			}
		case '^':
			switch (charAt(si)) {
			case '=':
				++si;
				return BITXOREQ;
			default:
				return BITXOR;
			}
		case '-':
			switch (charAt(si)) {
			case '-':
				++si;
				return DEC;
			case '=':
				++si;
				return SUBEQ;
			default:
				return SUB;
			}
		case '+':
			switch (charAt(si)) {
			case '+':
				++si;
				return INC;
			case '=':
				++si;
				return ADDEQ;
			default:
				return ADD;
			}
		case '/':
			switch (charAt(si)) {
			case '/':
				// rest of line is comment
				for (++si; si < source.length() && '\n' != charAt(si); ++si)
					;
				return COMMENT;
			case '*':
				for (++si; si < source.length() && (charAt(si) != '*' || charAt(si + 1) != '/'); ++si)
					;
				if (si < source.length())
					si += 2;
				return COMMENT;
			case '=':
				++si;
				return DIVEQ;
			default:
				return DIV;
			}
		case '*':
			if ('=' == charAt(si)) {
				++si;
				return MULEQ;
			} else
				return MUL;
		case '%':
			if ('=' == charAt(si)) {
				++si;
				return MODEQ;
			} else
				return MOD;
		case '$':
			if ('=' == charAt(si)) {
				++si;
				return CATEQ;
			} else
				return CAT;
		case '0':
			if (charAtLower(si) == 'x') {
				++si;
				while (-1 != Character.digit(charAt(si), 16))
					++si;
				// NOTE: this accepts "0x"
				return value(NUMBER);
			}
			// fall thru
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			while (Character.isDigit(charAt(si)))
				++si;
			if (charAt(si) == '.')
				++si;
			else if (charAtLower(si) != 'e')
				return value(NUMBER);
			// fall thru
		case '.':
			// NOTE: this accepts ".e1"
			while (Character.isDigit(charAt(si)))
				++si;
			if (c == '.' && si == prev + 1)
				return DOT;
			if (charAtLower(si) == 'e') {
				++si;
				if (charAt(si) == '+' || charAt(si) == '-')
					++si;
				while (Character.isDigit(charAt(si)))
					++si;
			}
			if (charAtLower(si - 1) == 'e')
				--si;
			if (charAt(si - 1) == '.')
				--si;
			return value(NUMBER);
		case '"' :
		case '\'' :
			char quote = c;
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
		default:
			if (Character.isLetter(c) || c == '_') {
				while (true) {
					c = charAt(si);
					if (Character.isLetterOrDigit(c) || c == '_')
						++si;
					else
						break;
				}
				if (c == '?' || c == '!')
					++si;
				value = source.substring(prev, si);
				keyword = ignoreCase
						? Token.lookupIgnoreCase(value) : Token.lookup(value);
				return keyword != null && keyword.isOperator()
						? keyword : IDENTIFIER;
			}
			return ERROR;
		}
	}

	private Token whitespace() {
		char c;
		boolean eol = false;
		for (; Character.isWhitespace(c = charAt(si)); ++si)
			if (c == '\r' || c == '\n')
				eol = true;
		return eol ? NEWLINE : WHITE;
	}

	private Token value(Token token) {
		value = source.substring(prev, si);
		return token;
	}

	private char doesc() {
		++si; // backslash
		int dig1, dig2, dig3;
		switch (charAt(si))
			{
		case 'n' :
			return '\n';
		case 't' :
			return '\t';
		case 'r' :
			return '\r';
		case 'x' :
			if (-1 != (dig1 = Character.digit(charAt(si + 1), 16)) && -1 != (dig2 = Character.digit(charAt(si + 2), 16))) {
				si += 2;
				return (char) (16 * dig1 + dig2);
				}
			else
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
				}
			else
				return source.charAt(--si);
			}
	}

	private char charAt(int i) {
		return i < source.length() ? source.charAt(i) : 0;
	}
	private char charAtLower(int i) {
		return i < source.length() ? Character.toLowerCase(source.charAt(i)) : 0;
	}
	public String remaining() {
		si = source.length();
		return source.substring(prev);
	}
}
