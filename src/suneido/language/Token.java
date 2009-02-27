/**
 *
 */
package suneido.language;

import static suneido.language.TokenFeature.ASSIGN;
import static suneido.language.TokenFeature.INFIX;

import java.util.EnumSet;

public enum Token {
	NIL, EOF, ERROR,
	IDENTIFIER, NUMBER, STRING,
	AND(INFIX), OR(INFIX),
	WHITE, COMMENT, NEWLINE,
	HASH, COMMA, COLON, SEMICOLON, Q_MARK(INFIX), AT, DOT,
	R_PAREN, L_PAREN(R_PAREN),
	R_BRACKET, L_BRACKET(R_BRACKET),
	R_CURLY, L_CURLY(R_CURLY),
	IS(INFIX), ISNT(INFIX), MATCH(INFIX), MATCHNOT(INFIX),
	LT(INFIX), LTE(INFIX), GT(INFIX), GTE(INFIX),
	NOT, INC, DEC, BITNOT,
	ADD(INFIX), SUB(INFIX), CAT(INFIX), MUL(INFIX), DIV(INFIX), MOD(INFIX),
	LSHIFT(INFIX), RSHIFT(INFIX), BITOR(INFIX), BITAND(INFIX), BITXOR(INFIX),
	EQ(INFIX, ASSIGN), 
	ADDEQ(INFIX, ASSIGN), SUBEQ(INFIX, ASSIGN), CATEQ(INFIX, ASSIGN), 
	MULEQ(INFIX, ASSIGN), DIVEQ(INFIX, ASSIGN), MODEQ(INFIX, ASSIGN), 
	LSHIFTEQ(INFIX, ASSIGN), RSHIFTEQ(INFIX, ASSIGN), 
	BITOREQ(INFIX, ASSIGN), BITANDEQ(INFIX, ASSIGN), BITXOREQ(INFIX, ASSIGN),
	// keywords
	IF, ELSE,
	WHILE, DO, FOR, FOREACH, FOREVER, BREAK, CONTINUE,
	SWITCH, CASE, DEFAULT,
	FUNCTION, CLASS,
	CATCH,
	DLL, STRUCT, CALLBACK,
	NEW, RETURN, TRY, THROW, SUPER,
	TRUE, FALSE, VALUE, IN, LIST, THIS,
	BOOL, CHAR, SHORT, LONG, INT64,
	FLOAT, DOUBLE, HANDLE, GDIOBJ,
	STRING_KEYWORD, BUFFER, RESOURCE, VOID;

	Token other;
	EnumSet<TokenFeature> features;
	Token() {
	}
	Token(Token other) {
		this.other = other;
		other.other = this;
	}
	Token(TokenFeature... has) {
		features = EnumSet.noneOf(TokenFeature.class);
		for (TokenFeature tf : has)
			features.add(tf);
	}

	public boolean isKeyword() {
		return ordinal() >= IF.ordinal();
	}
	public boolean infix() {
		return features != null && features.contains(INFIX);
	}
	public boolean assign() {
		return features != null && features.contains(ASSIGN);
	}
}