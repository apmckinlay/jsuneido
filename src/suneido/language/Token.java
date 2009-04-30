/**
 *
 */
package suneido.language;

import static suneido.language.TokenFeature.*;
import static suneido.language.TokenResultType.*;

import java.util.HashMap;
import java.util.Map;

public enum Token {
	NIL, EOF, ERROR,
	IDENTIFIER, NUMBER, STRING,
	AND("and", INFIX), OR("or", INFIX),
	WHITE, COMMENT, NEWLINE,
	HASH, COMMA, COLON, SEMICOLON, Q_MARK(INFIX), AT, DOT,
	R_PAREN, L_PAREN(R_PAREN),
	R_BRACKET, L_BRACKET(R_BRACKET),
	R_CURLY, L_CURLY(R_CURLY),
	IS("is", TERMOP, B), ISNT("isnt", TERMOP, B),
	MATCH("=~", INFIX, B), MATCHNOT("!~", INFIX, B),
	LT("<", TERMOP, B), LTE("<=", TERMOP, B),
	GT(">", TERMOP, B), GTE(">=", TERMOP, B),
	NOT("not"), INC, DEC, BITNOT("~"),
	ADD("+", INFIX, N), SUB("-", INFIX, N), CAT("$", INFIX, S),
	MUL("*", INFIX, N), DIV("/", INFIX, N), MOD("%", INFIX, N),
	LSHIFT("<<", INFIX, N), RSHIFT(">>", INFIX, N),
	BITOR("|", INFIX, N), BITAND("&", INFIX, N), BITXOR("^", INFIX, N),
	EQ("=", ASSIGN),
	ADDEQ("+=", ASSIGN, N), SUBEQ("-=", ASSIGN, N), CATEQ("$=", ASSIGN, S), MULEQ("*=", ASSIGN, N), DIVEQ("/=", ASSIGN, N), MODEQ("%=", ASSIGN, N),
	LSHIFTEQ("<<=", ASSIGN, N), RSHIFTEQ(">>=", ASSIGN, N),
	BITOREQ("|=", ASSIGN, N), BITANDEQ("&=", ASSIGN, N), BITXOREQ("^=", ASSIGN, N),
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
	TOTAL("total", SUMOP), SORT("sort"), PROJECT("project"), MAX("max", SUMOP),
	MIN("min", SUMOP), MINUS("minus"), INTERSECT("intersect"),
	LIST("list", SUMOP), UNION("union"), REMOVE("remove"), HISTORY("history"),
	EXTEND("extend"), COUNT("count", SUMOP), TIMES("times"), BY("by"),
	SUMMARIZE("summarize"), WHERE("where"), JOIN("join"), LEFTJOIN("leftjoin"),
	REVERSE("reverse"), AVERAGE("average", SUMOP),
	INTO("into"), INSERT("insert"), UPDATE("update"), SET("set");

	Token other;
	public String string;
	private TokenFeature feature;
	public TokenResultType resultType;
	public String method;


	Token() {
	}
	Token(Token other) {
		this.other = other;
		other.other = this;
	}
	Token(String string) {
		this(string, null);
	}
	Token(TokenFeature feature) {
		this(null, feature);
	}
	Token(String string, TokenFeature feature) {
		this.feature = feature;
		this.string = string;
	}
	Token(String string, TokenFeature feature, TokenResultType resultType) {
		this(string, feature);
		this.resultType = resultType;
		method = toString().replace("EQ", "").toLowerCase();
	}

	public boolean isOperator() {
		return ordinal() < IF.ordinal();
	}
	public boolean infix() {
		return feature == INFIX || feature == TERMOP || feature == ASSIGN;
	}
	public boolean assign() {
		return feature == ASSIGN;
	}
	public boolean sumop() {
		return feature == SUMOP;
	}
	public boolean termop() {
		return feature == TERMOP;
	}

	static final Map<String, Token> keywords = new HashMap<String, Token>();
	static {
		for (Token t : Token.values())
			if (t.string != null && Character.isLetter(t.string.charAt(0)))
				keywords.put(t.string, t);
		keywords.put("destroy", DROP);
	}
	public static Token lookup(String s) {
		return keywords.get(s);
	}
	public static Token lookupIgnoreCase(String s) {
		return keywords.get(s.toLowerCase());
	}
}