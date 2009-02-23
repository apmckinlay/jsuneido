package suneido.language;

import static suneido.language.Token.INC;
import suneido.SuValue;

public class StringGenerator implements Generator<String> {

	public String assignment(String text, String expression) {
		return text + " = (" + expression + ")";
	}

	public String conditional(String expression, String first, String second) {
		return "(" + expression + " ? " + first + " : " + second + ")";
	}

	public String constant(SuValue result) {
		return result.toString();
	}

	public String function(String statementList) {
		return "function () { " + str(statementList) + " }";
	}

	public String identifier(String text) {
		return text;
	}

	public String ifStatement(String expression, String t, String f) {
		return "if (" + expression + ") {" + str(" ", t) + " }" + str(" else { ", f, " }");
	}

	public String returnStatement(String expression) {
		return "return" + str(" ", expression) + ";";
	}

	public String expressionStatement(String expression) {
		return expression + ";";
	}

	public String statementList(String list, String statement) {
		return str("", list, " ") + str(statement);
	}

	public String whileStatement(String expression, String statement) {
		return "while (" + expression + ") {" + str(" ", statement) + " }";
	}

	public String dowhileStatement(String statement, String expression) {
		return "do {" + str(" ", statement) + " } while (" + expression + ");";
	}

	public String binaryExpression(Token op, String list, String next) {
		return "(" + str(list) + " " + op + " " + str(next) + ")";
	}

	public String unaryExpression(Token op, String expression) {
		return "(" + op + " " + expression + ")";
	}

	private String str(String x) {
		return x == null ? "" : (String) x;
	}
	private String str(String s, String x) {
		return x == null ? "" : s + x;
	}
	private String str(String s, String x, String t) {
		return x == null ? "" : s + x + t;
	}

	public String number(String value) {
		return "n(" + value + ")";
	}

	public String string(String value) {
		return "s(" + value + ")";
	}

	public String date(String value) {
		return "d(" + value + ")";
	}

	public String symbol(String value) {
		return "sym(" + value + ")";
	}

	public String bool(String value) {
		return value;
	}

	public String foreverStatement(String statement) {
		return "forever { " + statement.trim() + " }";
	}

	public String breakStatement() {
		return "break;";
	}

	public String continueStatement() {
		return "continue;";
	}

	public String throwStatement(String expression) {
		return "throw " + expression + ";";
	}

	public String catcher(String variable, String pattern, String statement) {
		return "catch" + str("(", variable, str(", '", pattern, "'") + ")")
				+ " { " + statement + " }";
	}

	public String tryStatement(String tryStatement, String catcher) {
		return "try { " + tryStatement + " }" + str(" ", catcher);
	}

	public String caseValues(String list, String expression) {
		return str("", list, ", ") + expression;
	}

	public String switchCases(String list, String values, String statements) {
		return str("", list, " ")
				+ (values == null ? "default:" : "case " + values + ":")
				+ " " + statements;
	}

	public String switchStatement(String expression, String cases) {
		return "switch (" + expression + ") {" + str(" ", cases) + " }";
	}

	public String forInStatement(String var, String expr, String statement) {
		return "for (" + var + " in " + expr + ") { " + statement + " }";
	}

	public String expressionList(String list, String expression) {
		return str("", list, ", ") + expression;
	}

	public String forClassicStatement(String expr1, String expr2, String expr3, String statement) {
		return "for (" + str(expr1) + "; " + str(expr2) + "; " + str(expr3) + ") { " + statement + " }";
	}

	public String preIncDec(Token incdec, String lvalue) {
		return (incdec == INC ? "++" : "--") + "(" + lvalue + ")";
	}

	public String postIncDec(Token incdec, String lvalue) {
		return "(" + lvalue + ")" + (incdec == INC ? "++" : "--");
	}

	public String member(String term, String identifier) {
		return term + "." + identifier;
	}

	public String subscript(String term, String expression) {
		return term + "[" + expression + "]";
	}

	public String self() {
		return "this";
	}

}
