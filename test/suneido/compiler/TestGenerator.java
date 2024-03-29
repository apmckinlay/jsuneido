/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import java.util.Map;

import suneido.SuObject;

public class TestGenerator extends Generator<Object> {

	private static void print(Object... args) {
		for (Object arg : args)
			if (arg != null)
				System.out.print(arg + " ");
		System.out.println();
	}

	@Override
	public Object and(Object expr1, Object expr2) {
		print("and(expr, expr)", expr1, expr2);
		return null;
	}

	@Override
	public Object andEnd(Object exprs) {
		print("andEnd");
		return "and-result";
	}

	@Override
	public Object argumentList(Object list, Object keyword, Object expression) {
		print("argumentList list=", list, "keyword=", keyword, expression);
		return "argumentList-result";
	}

	@Override
	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		print("binaryExpression", op, expr1, expr2);
		return null;
	}

	@Override
	public Object conditional(Object expr, Object first, Object second) {
		print("conditional", expr, first, second);
		return null;
	}

	@Override
	public Object functionCall(Object function, Object arguments) {
		print("functionCall", function, arguments);
		return null;
	}

	@Override
	public Object identifier(String text) {
		print("identifier", text);
		return "identifier=" + text;
	}

	@Override
	public Object in(Object expression, Object constant) {
		print("in", expression);
		return null;
	}

	@Override
	public Object object(SuObject members, int lineNumber) {
		print("object", members);
		return null;
	}

	@Override
	public Object or(Object expr1, Object expr2) {
		print("or", expr1, expr2);
		return null;
	}

	@Override
	public Object orEnd(Object exprs) {
		print("orEnd", exprs);
		return "or-result";
	}

	@Override
	public Object unaryExpression(Token op, Object expression) {
		print("unaryExpression", op, expression);
		return null;
	}

	@Override
	public Object assignment(Object term, Token op, Object expression) {
		print("assignment", term, op, expression);
		return "assignment-result";
	}

	@Override
	public Object clazz(String name, String base, Map<String,Object> members,
			int lineNumber) {
		print("classEnd", base, members);
		return "classEnd-result";
	}

	@Override
	public Object function(Object params, Object compound, boolean isMethod,
			int lineNumber) {
		print("functionEnd", params, compound);
		return "functionEnd-result";
	}

	@Override
	public Object parameters(Object list, String name, Object defaultValue) {
		print("parameters", name, defaultValue);
		return "parameters-result";
	}

	@Override
	public Object statementList(Object list, Object next) {
		print("statementList", next);
		return "statementList";
	}

	@Override
	public Object memberRef(Object term, String identifier, int lineNumber) {
		print("member", term, identifier);
		return "member-" + identifier;
	}

	@Override
	public Object returnStatement(Object expression, Object context,
			int lineNumber) {
		print("returnStatement", expression, context);
		return "returnStatement-result";
	}

	@Override
	public Object forInStatement(String var, Object expr, Object statement) {
		print("forInStatement", var, expr, statement);
		return "forInStatement-result";
	}

	@Override
	public Object switchStatement(Object expression, Object cases) {
		print("switchStatement");
		return null;
	}

	@Override
	public Object switchCases(Object cases, Object values, Object statements) {
		print("switchCases");
		return null;
	}

	@Override
	public Object caseValues(Object values, Object expression) {
		print("caseValues");
		return null;
	}

	@Override
	public Object catcher(String variable, String pattern, Object statement) {
		print("catcher", variable, pattern, statement);
		return null;
	}

	@Override
	public Object tryStatement(Object tryStatement, Object catcher) {
		print("tryStatement", tryStatement, catcher);
		return null;
	}

	@Override
	public Object selfRef(int lineNumber) {
		print("selfRef");
		return "selfRef-result";
	}

	@Override
	public Object rvalue(Object expr) {
		print("rvalue", expr);
		return "rvalue-" + expr;
	}

	public static void main(String[] args) {
		String s = "class { M() { } }";
		Lexer lexer = new Lexer(s);
		TestGenerator generator = new TestGenerator();
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<>(lexer, generator);
		pc.parse("test");
	}

	@Override
	public Object value(Object value) {
		return value;
	}

}
