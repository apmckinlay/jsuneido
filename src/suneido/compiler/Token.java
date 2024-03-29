/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.TokenFeature.ASSIGN;
import static suneido.compiler.TokenFeature.BINOP;
import static suneido.compiler.TokenFeature.SUMOP;
import static suneido.compiler.TokenFeature.TERMOP;
import static suneido.compiler.TokenResultType.*;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Ascii;

import suneido.util.Immutable;

@Immutable
public enum Token {
	NIL, EOF, ERROR,
	IDENTIFIER, NUMBER, STRING,
	AND("and", BINOP), OR("or", BINOP),
	WHITE, COMMENT, NEWLINE,
	HASH, COMMA, COLON, SEMICOLON, Q_MARK(BINOP), AT, DOT,
	R_PAREN, L_PAREN(R_PAREN),
	R_BRACKET, L_BRACKET(R_BRACKET),
	R_CURLY, L_CURLY(R_CURLY),
	IS("is", TERMOP, B), ISNT("isnt", TERMOP, B),
	MATCH("=~", BINOP, B), MATCHNOT("!~", BINOP, B),
	LT("<", TERMOP, B), LTE("<=", TERMOP, B),
	GT(">", TERMOP, B), GTE(">=", TERMOP, B),
	NOT("not", B), INC, DEC, BITNOT("~"),
	// NOTE: ADD and SUB are not BINOP because they can be unary
	ADD("+", N), SUB("-", N), CAT("$", BINOP, O),
	MUL("*", BINOP, N), DIV("/", BINOP, N), MOD("%", BINOP, N),
	LSHIFT("<<", BINOP, I), RSHIFT(">>", BINOP, I),
	BITOR("|", BINOP, I), BITAND("&", BINOP, I), BITXOR("^", BINOP, I),
	EQ("=", ASSIGN),
	ADDEQ("+=", ASSIGN, N), SUBEQ("-=", ASSIGN, N), CATEQ("$=", ASSIGN, O),
	MULEQ("*=", ASSIGN, N), DIVEQ("/=", ASSIGN, N), MODEQ("%=", ASSIGN, N),
	LSHIFTEQ("<<=", ASSIGN, I), RSHIFTEQ(">>=", ASSIGN, I),
	BITOREQ("|=", ASSIGN, I), BITANDEQ("&=", ASSIGN, I), BITXOREQ("^=", ASSIGN, I),
	RANGETO(".."), RANGELEN("::"),

	// keywords
	IF("if"), ELSE("else"),
	WHILE("while"), DO("do"), FOR("for"), FOREVER("forever"),
	BREAK("break"), CONTINUE("continue"),
	SWITCH("switch"), CASE("case"), DEFAULT("default"),
	FUNCTION("function"), CLASS("class"), CATCH("catch"),
	NEW("new"), RETURN("return"), TRY("try"), THROW("throw"),
	SUPER("super"), TRUE("true"), FALSE("false"),
	IN("in"), THIS("this"),

	// for queries
	ALTER("alter"), AVERAGE("average", SUMOP), BY("by"),
	CASCADE("cascade"), COUNT("count", SUMOP), CREATE("create"),
	DELETE("delete"), DROP("drop"), ENSURE("ensure"),
	EXTEND("extend"), HISTORY("history"), INDEX("index"),
	INSERT("insert"), INTERSECT("intersect"), INTO("into"),
	JOIN("join"), KEY("key"), LEFTJOIN("leftjoin"),
	LIST("list", SUMOP), MAX("max", SUMOP), MIN("min", SUMOP),
	MINUS("minus"), PROJECT("project"),	REMOVE("remove"),
	RENAME("rename"), REVERSE("reverse"), SET("set"),
	SORT("sort"), SUMMARIZE("summarize"), SVIEW("sview"),
	TIMES("times"), TO("to"), TOTAL("total", SUMOP),
	UPDATE("update"), UNION("union"), UNIQUE("unique"),
	VIEW("view"), WHERE("where"),

	// for AST
	CALL, MEMBER, SUBSCRIPT, ARG, FOR_IN, OBJECT, BINARYOP,
	SELFREF, ASSIGNOP, PREINCDEC, POSTINCDEC, BLOCK, RVALUE, METHOD,
	NOT_(NOT, "not_", B_), IS_(IS, "is_", B_), ISNT_(ISNT, "isnt_", B_),
	MATCH_(MATCH, "match_", B_), MATCHNOT_(MATCHNOT, "matchnot_", B_),
	LT_(LT, "lt_", B_), LTE_(LTE, "lte_", B_), GT_(GT, "gt_", B_), GTE_(GTE, "gte_", B_),
	CLOSURE, VALUE
	;

	Token other;
	public String string;
	private TokenFeature feature;
	public TokenResultType resultType;
	public String method;

	Token(Object... args) {
		for (Object arg : args) {
			if (arg instanceof String)
				this.string = (String) arg;
			else if (arg instanceof TokenFeature)
				this.feature = (TokenFeature) arg;
			else if (arg instanceof Token) {
				this.other = (Token) arg;
				other.other = this;
			} else if (arg instanceof TokenResultType) {
				this.resultType = (TokenResultType) arg;
				method = Ascii.toLowerCase(toString().replace("EQ", ""));
			}
		}
	}

	public boolean isOperator() {
		return ordinal() < IF.ordinal();
	}
	public boolean binop() {
		return feature == BINOP || feature == TERMOP || feature == ASSIGN;
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

	static final Map<String, Token> keywords = new HashMap<>();
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
		return keywords.get(Ascii.toLowerCase(s));
	}
}
