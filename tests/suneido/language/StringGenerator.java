package suneido.language;

import static suneido.language.Generator.ObjectOrRecord.OBJECT;
import static suneido.language.Token.*;
import suneido.SuValue;
import suneido.language.ParseExpression.Value;

public class StringGenerator implements Generator<String> {

	public String assignment(String term, Value<String> value, Token op,
			String expression) {
		return str("", term, " ") + expression + " " + op + "(" + value(value)
				+ ")";
	}

	public String conditional(String expression, String first, String second,
			Object label) {
		return "(" + expression + " ? " + first + " : " + second + ")";
	}

	public String constant(SuValue result) {
		return result.toString();
	}

	public String function(String parameters, String statementList) {
		return "function (" + str(parameters) + ") { "
				+ str("", statementList, " ") + "}";
	}

	public String identifier(String text) {
		return text;
	}

	public String ifStatement(String expr, String t, String f, Object label) {
		return "if (" + expr + ") {" + str(" ", t, "") + " }"
				+ str(" else { ", f, " }");
	}

	public String returnStatement(String expression, Object context) {
		return "return" + str(" ", expression, "") + ";";
	}

	public String expressionStatement(String expression) {
		return expression + ";";
	}

	public String statementList(String list, String statement) {
		return str("", list, " ") + str(statement);
	}

	public String whileStatement(String expression, String statement,
			Object loop) {
		return "while (" + expression + ") {" + str(" ", statement, "") + " }";
	}

	public String dowhileStatement(String body, String expr, Object label) {
		return "do {" + str(" ", body, "") + " } while (" + expr + ");";
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

	public String foreverStatement(String statement, Object label) {
		return "forever { " + statement.trim() + " }";
	}

	public String breakStatement(Object loop) {
		return "break;";
	}

	public String continueStatement(Object loop) {
		return "continue;";
	}

	public String throwStatement(String expression) {
		return "throw " + expression + ";";
	}

	public String catcher(String variable, String pattern, String statement) {
		return "catch" + str("(", variable, str(", '", pattern, "'") + ")")
				+ " { " + statement + " }";
	}

	public String tryStatement(String tryStatement, String catcher,
			Object trycatch) {
		return "try { " + tryStatement + " }" + str(" ", catcher, "");
	}

	public String caseValues(String list, String expression, Object labels,
			boolean more) {
		return str("", list, ", ") + expression;
	}
	public String switchCases(String list, String values, String statements,
			Object labels) {
		return str("", list, " ")
				+ (values == null ? "default:" : "case " + values + ":")
				+ " " + statements;
	}
	public String switchStatement(String expression, String cases, Object labels) {
		return "switch (" + expression + ") {" + str(" ", cases, "") + " }";
	}

	public String forInStatement(String var, String expr, String statement,
			Object loop) {
		return "for (" + var + " in " + expr + ") { " + statement + " }";
	}

	public String expressionList(String list, String expression) {
		return str("", list, ", ") + expression;
	}

	public String forClassicStatement(String expr1, String expr2, String expr3, String statement, Object loop) {
		return "for (" + str(expr1) + "; " + str(expr2) + "; " + str(expr3) + ") { " + statement + " }";
	}

	public String preIncDec(String term, Token incdec, Value<String> value) {
		return str("", term, " ") + "pre" + incdec + "(" + value(value) + ")";
	}

	public String postIncDec(String term, Token incdec, Value<String> value) {
		return str("", term, " ") + "post" + incdec + "(" + value(value) + ")";
	}

	private String value(Value<String> value) {
		if (value.type == null)
			return null;
		switch (value.type) {
		case IDENTIFIER:
			return value.id;
		case MEMBER:
			return "." + value.id;
		case SUBSCRIPT:
			return "[" + value.expr + "]";
		}
		return null;
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

	public String functionCall(String function, Value<String> value,
			String arguments) {
		return function + str(" ", value(value), "") + "(" + str(arguments)
				+ ")";
	}

	public String newExpression(String term, String arguments) {
		return "new " + term + str("(", arguments, ")");
	}

	public void atArgument(String n) {
	}
	public String atArgument(String n, String expression) {
		return "@" + ("0".equals(n) ? "" : str("+", n, " ")) + expression;
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

	public Object startFunction(FuncOrBlock which) {
		return null;
	}

	public void lvalue(Value<String> value) {
	}

	public void afterStatement(String statements) {
	}

	public void argumentName(String keyword) {
	}

	public void preFunctionCall(Value<String> value) {
	}

	public Object and(Object prevlabel) {
		return null;
	}
	public void andEnd(Object label) {
	}

	public Object or(Object label) {
		return null;
	}
	public void orEnd(Object label) {
	}

	public Object ifExpr(String expr) {
		return null;
	}
	public void ifThen(Object label, String t) {
	}
	public Object ifElse(Object label) {
		return null;
	}

	public Object conditionalTrue(Object label, String first) {
		return null;
	}

	public Object loop() {
		return true; // can't be null
	}

	public void whileExpr(String expr, Object loop) {
	}

	public void newCall() {
	}

	public Object forStart() {
		return null;
	}
	public void forIncrement(Object label) {
	}
	public void forCondition(String cond, Object loop) {
	}

	public void startCase(Object labels) {
	}
	public void startCaseBody(Object labels) {
	}
	public Object startSwitch() {
		return null;
	}
	public void startCaseValue() {
	}

	public Object forInExpression(String var, String expr) {
		return null;
	}

	public void blockParams() {
	}

	public void startCatch(String var, String pattern, Object trycatch) {
	}

	public Object startTry() {
		return null;
	}

}
