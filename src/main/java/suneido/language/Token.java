/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.TokenFeature.ASSIGN;
import static suneido.language.TokenFeature.INFIX;
import static suneido.language.TokenFeature.SUMOP;
import static suneido.language.TokenFeature.TERMOP;
import static suneido.language.TokenResultType.*;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import suneido.language.jsdi.DllInterface;

@Immutable
public enum Token {
	NIL, EOF, ERROR(1000),
	IDENTIFIER(1001), NUMBER(1002), STRING(1003),
	AND("and", INFIX, 1004), OR("or", INFIX, 1005),
	WHITE(1006), COMMENT(1007), NEWLINE(1008),
	HASH(35), COMMA(44), COLON(58), SEMICOLON(59), Q_MARK(INFIX, 63), AT(64), DOT(46),
	R_PAREN(41), L_PAREN(R_PAREN, 40),
	R_BRACKET(93), L_BRACKET(R_BRACKET, 91),
	R_CURLY(125), L_CURLY(R_CURLY, 123),
	IS("is", TERMOP, B, 251), ISNT("isnt", TERMOP, B, 252),
	MATCH("=~", INFIX, B, 253), MATCHNOT("!~", INFIX, B, 254),
	LT("<", TERMOP, B, 6), LTE("<=", TERMOP, B, 7),
	GT(">", TERMOP, B, 8), GTE(">=", TERMOP, B, 9),
	NOT("not", B, 10), INC(140), DEC(142), BITNOT("~", 11),
	ADD("+", INFIX, N, 240), SUB("-", INFIX, N, 241), CAT("$", INFIX, O, 242),
	MUL("*", INFIX, N, 243), DIV("/", INFIX, N, 244), MOD("%", INFIX, N, 245),
	LSHIFT("<<", INFIX, I, 246), RSHIFT(">>", INFIX, I, 247),
	BITOR("|", INFIX, I, 248), BITAND("&", INFIX, I, 249), BITXOR("^", INFIX, I, 250),
	EQ("=", ASSIGN, 139),
	ADDEQ("+=", ASSIGN, N, 128), SUBEQ("-=", ASSIGN, N, 129), CATEQ("$=", ASSIGN, O, 130),
	MULEQ("*=", ASSIGN, N, 131), DIVEQ("/=", ASSIGN, N, 132), MODEQ("%=", ASSIGN, N, 133),
	LSHIFTEQ("<<=", ASSIGN, I, 134), RSHIFTEQ(">>=", ASSIGN, I, 135),
	BITOREQ("|=", ASSIGN, I, 136), BITANDEQ("&=", ASSIGN, I, 137), BITXOREQ("^=", ASSIGN, I, 138),
	RANGETO(".."), RANGELEN("::"),
	// keywords
	IF("if", 1), ELSE("else", 2),
	WHILE("while", 3), DO("do", 4), FOR("for", 5), FOREVER("forever", 7),
	BREAK("break", 8), CONTINUE("continue", 9),
	SWITCH("switch", 10), CASE("case", 11), DEFAULT("default", 12),
	FUNCTION("function", 13), CLASS("class", 14), CATCH("catch", 15),
	@DllInterface DLL("dll", 16), @DllInterface STRUCT("struct", 17),
	@DllInterface CALLBACK("callback", 18),
	NEW("new", 19), RETURN("return", 20), TRY("try", 21), THROW("throw", 22),
	SUPER("super", 23), TRUE("true", 24), FALSE("false", 25),
	IN("in", 27), THIS("this", 29),
	// for queries
	ALTER("alter", 43), AVERAGE("average", SUMOP, 44), BY("by", 45), 
	CASCADE("cascade", 46), COUNT("count", SUMOP, 47), CREATE("create", 48), 
	DELETE("delete", 49), DROP("drop", 50), ENSURE("ensure", 51), 
	EXTEND("extend", 52), HISTORY("history", 53), INDEX("index", 54), 
	INSERT("insert", 55), INTERSECT("intersect", 56), INTO("into", 57), 
	JOIN("join", 58), KEY("key", 59), LEFTJOIN("leftjoin", 60),
	LIST("list", SUMOP, 61), MAX("max", SUMOP, 62), MIN("min", SUMOP, 63),
	MINUS("minus", 64), PROJECT("project", 65),	REMOVE("remove", 66), 
	RENAME("rename", 67), REVERSE("reverse", 68), SET("set", 69), 
	SORT("sort", 70), SUMMARIZE("summarize", 71), SVIEW("sview", 72), 
	TIMES("times", 73), TO("to", 74), TOTAL("total", SUMOP, 75), 
	UPDATE("update", 76), UNION("union", 77), UNIQUE("unique", 78), 
	VIEW("view", 79), WHERE("where", 80),
	
	// for AST
	DATE, SYMBOL, CALL, MEMBER, SUBSCRIPT, ARG, FOR_IN, RECORD, OBJECT, BINARYOP,
	SELFREF, ASSIGNOP, PREINCDEC, POSTINCDEC, BLOCK, RVALUE, METHOD,
	NOT_(NOT, "not_", B_), IS_(IS, "is_", B_), ISNT_(ISNT, "isnt_", B_),
	MATCH_(MATCH, "match_", B_), MATCHNOT_(MATCHNOT, "matchnot_", B_),
	LT_(LT, "lt_", B_), LTE_(LTE, "lte_", B_), GT_(GT, "gt_", B_), GTE_(GTE, "gte_", B_),
	CLOSURE,
	// for AST (DLL interface)
	@DllInterface VALUETYPE, @DllInterface ARRAYTYPE,
	@DllInterface POINTERTYPE,
	// TODO include long, bool, etc. for compatibility with cSuneido Scanner
	;

	Token other;
	public String string;
	private TokenFeature feature;
	public TokenResultType resultType;
	public String method;
	public int oldnum = 0;

	Token(Object... args) {
		for (Object arg : args) {
			if (arg instanceof String)
				this.string = (String) arg;
			else if (arg instanceof Integer)
				this.oldnum = (Integer) arg;
			else if (arg instanceof TokenFeature)
				this.feature = (TokenFeature) arg;
			else if (arg instanceof Token) {
				this.other = (Token) arg;
				other.other = this;
			} else if (arg instanceof TokenResultType) {
				this.resultType = (TokenResultType) arg;
				method = toString().replace("EQ", "").toLowerCase();
			}
		}
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
		keywords.put("xor", ISNT);
	}
	public static Token lookup(String s) {
		return keywords.get(s);
	}
	public static Token lookupIgnoreCase(String s) {
		return keywords.get(s.toLowerCase());
	}
}