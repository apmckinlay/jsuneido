/**
 *
 */
package suneido.language;

import static suneido.language.TokenFeature.ASSIGN;
import static suneido.language.TokenFeature.INFIX;

import java.util.*;

public enum Token {
	NIL, EOF, ERROR,
	IDENTIFIER, NUMBER, STRING,
	AND("and", INFIX), OR("or", INFIX),
	WHITE, COMMENT, NEWLINE,
	HASH, COMMA, COLON, SEMICOLON, Q_MARK(INFIX), AT, DOT,
	R_PAREN, L_PAREN(R_PAREN),
	R_BRACKET, L_BRACKET(R_BRACKET),
	R_CURLY, L_CURLY(R_CURLY),
	IS("is", INFIX), ISNT("isnt", INFIX), MATCH(INFIX), MATCHNOT(INFIX),
	LT(INFIX), LTE(INFIX), GT(INFIX), GTE(INFIX),
	NOT("not"), INC, DEC, BITNOT,
	ADD(INFIX), SUB(INFIX), CAT(INFIX), MUL(INFIX), DIV(INFIX), MOD(INFIX),
	LSHIFT(INFIX), RSHIFT(INFIX), BITOR(INFIX), BITAND(INFIX), BITXOR(INFIX),
	EQ(ASSIGN),
	ADDEQ(ASSIGN), SUBEQ(ASSIGN), CATEQ(ASSIGN),
	MULEQ(ASSIGN), DIVEQ(ASSIGN), MODEQ(ASSIGN),
	LSHIFTEQ(ASSIGN), RSHIFTEQ(ASSIGN),
	BITOREQ(ASSIGN), BITANDEQ(ASSIGN), BITXOREQ(ASSIGN),
	// keywords
	IF("if"), ELSE("else"),
	WHILE("while"), DO("do"), FOR("for"), FOREVER("forever"),
	BREAK("break"), CONTINUE("continue"),
	SWITCH("switch"), CASE("case"), DEFAULT("default"),
	FUNCTION("function"), CLASS("class"),
	CATCH("catch"),
	DLL("dll"), STRUCT("struct"), CALLBACK("callback"),
	NEW("new"), RETURN("return"), TRY("try"), THROW("throw"), SUPER("super"),
	TRUE("true"), FALSE("false"), IN("in"), THIS("this"),
	// for queries
	VIEW("view"), SVIEW("sview"), CREATE("create"), ENSURE("ensure"),
	DROP("drop"), ALTER("alter"), DELETE("delete"),
	RENAME("rename"), TO("to"), UNIQUE("unique"), LOWER("lower"),
	CASCADE("cascade"), UPDATES("updates"), INDEX("index"), KEY("key"),
	TOTAL("total"), SORT("sort"), PROJECT("project"), MAX("max"), MIN("min"),
	MINUS("minus"), INTERSECT("intersect"), LIST("list"), UNION("union"),
	REMOVE("remove"), HISTORY("history"), EXTEND("extend"), COUNT("count"),
	TIMES("times"), BY("by"), SUMMARIZE("summarize"), WHERE("where"),
	JOIN("join"), LEFTJOIN("leftjoin"), REVERSE("reverse"), AVERAGE("average"),
	INTO("into"), INSERT("insert"), UPDATE("update"), SET("set");

	Token other;
	String keyword;
	TokenFeature feature;

	Token() {
	}
	Token(Token other) {
		this.other = other;
		other.other = this;
	}
	Token(String keyword) {
		this.keyword = keyword;
	}
	Token(TokenFeature feature) {
		this.feature = feature;
	}
	Token(String keyword, TokenFeature feature) {
		this.keyword = keyword;
		this.feature = feature;
	}

	public boolean isOperator() {
		return ordinal() < IF.ordinal();
	}
	public boolean infix() {
		return feature == INFIX || feature == ASSIGN;
	}
	public boolean assign() {
		return feature == ASSIGN;
	}

	static final Map<String, Token> keywords = new HashMap<String, Token>();
	static {
		for (Token t : Token.values())
			if (t.keyword != null)
				keywords.put(t.keyword, t);
		keywords.put("destroy", DROP);
	}
	public static Token lookup(String s) {
		return keywords.get(s);
	}
	public static Token lookupIgnoreCase(String s) {
		return keywords.get(s.toLowerCase());
	}
}