package suneido.language;

import static suneido.language.Generator.ObjectOrRecord.OBJECT;
import static suneido.language.Token.*;
import suneido.SuValue;

public class StringGenerator implements Generator<String> {

	public String assignment(String lvalue, Token op, String expression) {
		return expression + " " + op + "(" + lvalue + ")";
	}

	public String conditional(String expression, String first, String second) {
		return "(" + expression + " ? " + first + " : " + second + ")";
	}

	public String constant(SuValue result) {
		return result.toString();
	}

	public String function(String parameters, String statementList) {
		return "function (" + str(parameters) + ") { " + str("", statementList, " ") + "}";
	}

	public String identifier(String text) {
		return text;
	}

	public String ifStatement(String expression, String t, String f) {
		return "if (" + expression + ") {" + str(" ", t, "") + " }" + str(" else { ", f, " }");
	}

	public String returnStatement(String expression) {
		return "return" + str(" ", expression, "") + ";";
	}

	public String expressionStatement(String expression) {
		return expression + ";";
	}

	public String statementList(String list, String statement) {
		return str("", list, " ") + str(statement);
	}

	public String whileStatement(String expression, String statement) {
		return "while (" + expression + ") {" + str(" ", statement, "") + " }";
	}

	public String dowhileStatement(String statement, String expression) {
		return "do {" + str(" ", statement, "") + " } while (" + expression + ");";
	}

	public String binaryExpression(Token op, String expr1, String expr2) {
		return str(expr1) + " " + str(expr2) + " " + op;
	}

	public String unaryExpression(Token op, String expression) {
		return expression + " " + (op == ADD | op == SUB ? "u" : "") + op;
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

	public String bool(boolean value) {
		return "b(" + value + ")";
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
		return "try { " + tryStatement + " }" + str(" ", catcher, "");
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
		return "switch (" + expression + ") {" + str(" ", cases, "") + " }";
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
		return "pre" + incdec + "(" + lvalue + ")";
	}

	public String postIncDec(Token incdec, String lvalue) {
		return "post" + incdec + "(" + lvalue + ")";
	}

	public String member(String term, String identifier) {
		return term + " ." + identifier;
	}

	public String subscript(String term, String expression) {
		return term + " " + expression + " []";
	}

	public String self() {
		return "this";
	}

	public String functionCall(String function, String arguments) {
		return function + "(" + str(arguments) + ")";
	}

	public String newExpression(String term, String arguments) {
		return "new " + term + str("(", arguments, ")");
	}

	public String atArgument(String n, String expression) {
		return "@" + str("+", n, " ") + expression;
	}

	public String block(String params, String statements) {
		return "{" + str("|", params, "|") + " " + str("", statements, " ") + "}";
	}

	public String parameters(String list, String name, String defaultValue) {
		return str("", list, ", ") + name + str(" = ", defaultValue, "");
	}

	public String argumentList(String list, String keyword, String expression) {
		return str("", list, ", ") + str("", keyword, ": ") + expression;
	}

	public String classConstant(String base, String members) {
		if ("Object".equals(base))
			base = null;
		return "class" + str(" : ", base, "") + " { " + str("", members, " ") + "}";
	}

	public String memberDefinition(String name, String value) {
		return str("", name, ": ") + value;
	}

	public String memberList(ObjectOrRecord which, String list, String member) {
		return str("", list, ", ") + member;
	}

	public String object(ObjectOrRecord which, String members) {
		return "#" + (which == OBJECT ? "(" : "{") + str(members)
				+ (which == OBJECT ? ")" : "}");
	}

	private String str(String x) {
		return x == null ? "" : (String) x;
	}

	protected String str(String s, String x, String t) {
		return x == null ? "" : s + x + t;
	}

	public String and(String expr1, String expr2) {
		return binaryExpression(AND, expr1, expr2);
	}

	public String or(String expr1, String expr2) {
		return binaryExpression(OR, expr1, expr2);
	}

	public String in(String expression, String constant) {
		if (constant == null)
			return expression + " in";
		else
			return expression + " " + constant;
	}

	public String constant(String value) {
		return value;
	}
}
