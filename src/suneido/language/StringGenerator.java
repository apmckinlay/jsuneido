package suneido.language;

import suneido.SuString;
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
		return "function () { " + str(statementList) + "}";
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

	public String statementList(String n, String next) {
		return str(n) + str(next) + " ";
	}

	public String whileStatement(String expression, String statement) {
		return "while (" + expression + ") {" + str(" ", statement) + " }";
	}

	public String dowhileStatement(String statement, String expression) {
		return "do {" + str(" ", statement) + " } while (" + expression + ")";
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

}
