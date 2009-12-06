package suneido.database.query;

import static suneido.language.Generator.MType.OBJECT;
import static suneido.language.Token.*;
import suneido.SuValue;
import suneido.language.Token;
import suneido.language.ParseExpression.Value;

public class StringGenerator extends QueryGenerator<String> {

	@Override
	public String assignment(String term, Value<String> value, Token op,
			String expression) {
		return str("", term, " ") + expression + " " + op + "(" + value(value)
				+ ")";
	}

	@Override
	public String conditional(String expression, String first, String second,
			Object label) {
		return "(" + expression + " ? " + first + " : " + second + ")";
	}

	public String constant(SuValue result) {
		return result.toString();
	}

	@Override
	public String function(String parameters, String statementList) {
		return "function (" + str(parameters) + ") { "
				+ str("", statementList, " ") + "}";
	}

	@Override
	public String identifier(String text) {
		return text;
	}

	@Override
	public String ifStatement(String expr, String t, String f, Object label) {
		return "if (" + expr + ") {" + str(" ", t, "") + " }"
				+ str(" else { ", f, " }");
	}

	@Override
	public String returnStatement(String expression, Object context) {
		return "return" + str(" ", expression, "") + ";";
	}

	@Override
	public String expressionStatement(String expression) {
		return expression + ";";
	}

	@Override
	public String statementList(String list, String statement) {
		return str("", list, " ") + str(statement);
	}

	@Override
	public String whileStatement(String expression, String statement,
			Object loop) {
		return "while (" + expression + ") {" + str(" ", statement, "") + " }";
	}

	@Override
	public String dowhileStatement(String body, String expr, Object label) {
		return "do {" + str(" ", body, "") + " } while (" + expr + ");";
	}

	@Override
	public String binaryExpression(Token op, String expr1, String expr2) {
		return str(expr1) + " " + str(expr2) + " " + op;
	}

	@Override
	public String unaryExpression(Token op, String expression) {
		return expression + " " + (op == ADD | op == SUB ? "u" : "") + op;
	}

	@Override
	public String number(String value) {
		return "n(" + value + ")";
	}

	@Override
	public String string(String value) {
		return "s(" + value + ")";
	}

	@Override
	public String date(String value) {
		return "d(" + value + ")";
	}

	@Override
	public String symbol(String value) {
		return "sym(" + value + ")";
	}

	@Override
	public String bool(boolean value) {
		return "b(" + value + ")";
	}

	@Override
	public String foreverStatement(String statement, Object label) {
		return "forever { " + statement.trim() + " }";
	}

	@Override
	public String breakStatement(Object loop) {
		return "break;";
	}

	@Override
	public String continueStatement(Object loop) {
		return "continue;";
	}

	@Override
	public String throwStatement(String expression) {
		return "throw " + expression + ";";
	}

	@Override
	public String catcher(String variable, String pattern, String statement) {
		return "catch" + str("(", variable, str(", '", pattern, "'") + ")")
				+ " { " + statement + " }";
	}

	@Override
	public String tryStatement(String tryStatement, String catcher,
			Object trycatch) {
		return "try { " + tryStatement + " }" + str(" ", catcher, "");
	}

	@Override
	public String caseValues(String list, String expression, Object labels,
			boolean more) {
		return str("", list, ", ") + expression;
	}

	@Override
	public String switchCases(String list, String values, String statements,
			Object labels) {
		return str("", list, " ")
				+ (values == null ? "default:" : "case " + values + ":") + " "
				+ statements;
	}

	@Override
	public String switchStatement(String expression, String cases, Object labels) {
		return "switch (" + expression + ") {" + str(" ", cases, "") + " }";
	}

	@Override
	public String forInStatement(String var, String expr, String statement,
			Object loop) {
		return "for (" + var + " in " + expr + ") { " + statement + " }";
	}

	@Override
	public String expressionList(String list, String expression) {
		return str("", list, ", ") + expression;
	}

	@Override
	public String forClassicStatement(String expr1, String expr2, String expr3,
			String statement, Object loop) {
		return "for (" + str(expr1) + "; " + str(expr2) + "; " + str(expr3)
				+ ") { " + statement + " }";
	}

	@Override
	public String preIncDec(String term, Token incdec, Value<String> value) {
		return str("", term, " ") + "pre" + incdec + "(" + value(value) + ")";
	}

	@Override
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

	@Override
	public String member(String term, Value<String> value) {
		return term + " ." + value.id;
	}

	@Override
	public String subscript(String term, String expression) {
		return term + " " + expression + " []";
	}

	@Override
	public String selfRef() {
		return "this";
	}

	@Override
	public String superRef() {
		return "super";
	}

	@Override
	public String functionCall(String function, Value<String> value,
			String arguments) {
		return function + str(" ", value(value), "") + "(" + str(arguments)
				+ ")";
	}

	@Override
	public String newExpression(String term, String arguments) {
		return "new " + term + str("(", arguments, ")");
	}

	@Override
	public String atArgument(String n, String expression) {
		return "@" + ("0".equals(n) ? "" : str("+", n, " ")) + expression;
	}

	@Override
	public String block(String params, String statements) {
		return "{" + str("|", params, "|") + " " + str("", statements, " ")
				+ "}";
	}

	@Override
	public String parameters(String list, String name, String defaultValue) {
		return str("", list, ", ") + name + str(" = ", defaultValue, "");
	}

	@Override
	public String argumentList(String list, Object keyword, String expression) {
		return str("", list, ", ") + str("", keyword, ": ")
				+ expression;
	}

	@Override
	public String classConstant(String base, String members) {
		if ("Object".equals(base))
			base = null;
		return "class" + str(" : ", base, "") + " { " + str("", members, " ")
				+ "}";
	}

	@Override
	public String memberDefinition(String name, String value) {
		return str("", name, ": ") + value;
	}

	@Override
	public String memberList(MType which, String list, String member) {
		return str("", list, ", ") + member;
	}

	@Override
	public String object(MType which, String members) {
		return "#" + (which == OBJECT ? "(" : "{") + str(members)
				+ (which == OBJECT ? ")" : "}");
	}

	private String str(String x) {
		return x == null ? "" : (String) x;
	}

	protected String str(String s, Object x, String t) {
		return x == null ? "" : s + x.toString() + t;
	}

	@Override
	public String and(String expr1, String expr2) {
		return binaryExpression(AND, expr1, expr2);
	}

	@Override
	public String or(String expr1, String expr2) {
		return binaryExpression(OR, expr1, expr2);
	}

	@Override
	public String in(String expression, String constant) {
		if (constant == null)
			return expression + " in";
		else
			return expression + " " + constant;
	}

	@Override
	public String constant(String value) {
		return value;
	}

	@Override
	public String rvalue(String expr) {
		return expr;
	}

	@Override
	public String columns(String columns, String column) {
		return str("", columns, ", ") + column;
	}

	@Override
	public String delete(String query) {
		return "delete " + query;
	}

	@Override
	public String history(String table) {
		return "history(" + table + ")";
	}

	@Override
	public String insertQuery(String query, String table) {
		return "insert " + query + " into " + table;
	}

	@Override
	public String insertRecord(String record, String query) {
		return "insert " + record + " into " + query;
	}

	@Override
	public String sort(String query, boolean reverse, String columns) {
		return query + " sort" + (reverse ? " reverse" : "") + " " + columns;
	}

	@Override
	public String table(String table) {
		return table;
	}

	@Override
	public String update(String query, String updates) {
		return "update " + query + " set " + updates;
	}

	@Override
	public String updates(String updates, String column, String expr) {
		return str("", updates, ", ") + column + " = " + expr;
	}

	@Override
	public String project(String query, String columns) {
		return query + " project " + columns;
	}

	@Override
	public String remove(String query, String columns) {
		return query + " remove " + columns;
	}

	@Override
	public String times(String query1, String query2) {
		return query1 + " times " + query2;
	}

	@Override
	public String union(String query1, String query2) {
		return query1 + " union " + query2;
	}

	@Override
	public String minus(String query1, String query2) {
		return query1 + " minus " + query2;
	}

	@Override
	public String intersect(String query1, String query2) {
		return query1 + " intersect " + query2;
	}

	@Override
	public String join(String query1, String by, String query2) {
		return query1 + " join " + str("by(", by, ") ") + query2;
	}

	@Override
	public String leftjoin(String query1, String by, String query2) {
		return query1 + " leftjoin " + str("by(", by, ") ") + query2;
	}

	@Override
	public String rename(String query, String renames) {
		return query + " rename " + renames;
	}

	@Override
	public String renames(String renames, String from, String to) {
		return str("", renames, ", ") + from + " to " + to;
	}

	@Override
	public String extend(String query, String list) {
		return query + " extend " + list;
	}

	@Override
	public String extendList(String list, String column, String expr) {
		return str("", list, ", ") + column + " = " + expr;
	}

	@Override
	public String where(String query, String expr) {
		return query + " where " + expr;
	}

	@Override
	public String summarize(String query, String by, String ops) {
		return query + " summarize " + str("", by, ", ") + ops;
	}

	@Override
	public String sumops(String sumops, String name, Token op, String field) {
		return str("", sumops, ", ") + str("", name, " = ") + op.string
				+ str(" ", field, "");
	}

}
