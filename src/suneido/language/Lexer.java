package suneido.language;

import static suneido.language.Token.*;

public class Lexer {
	private final String source;
	private int si = 0;

	public Lexer(String source) {
		this.source = source;
	}

	public Token next() {
		Token token;
		do
			token = nextAll();
			while (token == WHITE || token == COMMENT);
		return token;
	}

	public Token nextAll() {
		if (si >= source.length())
			return EOF;
		char c = source.charAt(si);
		if (Character.isWhitespace(c)) {
			boolean eol = false;
			for (; Character.isWhitespace(c = charAt(si)); ++si)
				if (c == '\r' || c == '\n')
					eol = true;
			return eol ? NEWLINE : WHITE;
		}
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
			default : return EQ;
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
				return NUMBER;
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
				return NUMBER;
			// fall thru
		case '.':
			// NOTE: this accepts ".e1"
			while (Character.isDigit(charAt(si)))
				++si;
			if (charAtLower(si) == 'e') {
				++si;
				if (charAt(si) == '+' || charAt(si) == '-')
					++si;
				while (Character.isDigit(charAt(si)))
					++si;
			}
			return NUMBER;
		case '"' :
		case '\'' :
			char quote = c;
			for (; si < source.length() && source.charAt(si) != quote; ++si)
				if ('\\' == charAt(si))
					doesc();
			if (si < source.length())
				++si;	// skip closing quote
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
				return IDENTIFIER;
			}
			return ERROR;
		}
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
			if (-1 != (dig1 = Character.digit(charAt(si), 16)) &&
					-1 != (dig2 = Character.digit(charAt(si + 1), 16))) {
				si += 2;
				return (char) (16 * dig2 + dig1);
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
				return (char) (64 * dig3 + 8 * dig2 + dig1);
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

}
