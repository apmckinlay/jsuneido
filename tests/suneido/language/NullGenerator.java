package suneido.language;

import suneido.SuValue;

public class NullGenerator implements Generator<Object> {

	public Object argumentList(Object list, String keyword, Object expression) {
		return true;
	}

	public Object assignment(Object term, Token op, Object expression) {
		return true;
	}

	public Object atArgument(String n, Object expr) {
		return true;
	}

	public Object binaryExpression(Token op, Object list, Object next) {
		return true;
	}

	public Object block(Object params, Object statements) {
		return true;
	}

	public Object bool(String value) {
		return true;
	}

	public Object breakStatement() {
		return true;
	}

	public Object caseValues(Object values, Object expression) {
		return true;
	}

	public Object catcher(String variable, String pattern, Object statement) {
		return true;
	}

	public Object classConstant(String base, Object members) {
		return true;
	}

	public Object conditional(Object primaryExpression, Object first, Object second) {
		return true;
	}

	public Object constant(SuValue result) {
		return true;
	}

	public Object continueStatement() {
		return true;
	}

	public Object date(String value) {
		return true;
	}

	public Object dowhileStatement(Object statement, Object expression) {
		return true;
	}

	public Object expressionList(Object list, Object expression) {
		return true;
	}

	public Object expressionStatement(Object expression) {
		return true;
	}

	public Object forClassicStatement(Object expr1, Object expr2, Object expr3, Object statement) {
		return true;
	}

	public Object forInStatement(String var, Object expr, Object statement) {
		return true;
	}

	public Object foreverStatement(Object statement) {
		return true;
	}

	public Object function(Object params, Object compound) {
		return true;
	}

	public Object functionCall(Object function, Object arguments) {
		return true;
	}

	public Object identifier(String text) {
		return true;
	}

	public Object ifStatement(Object expression, Object t, Object f) {
		return true;
	}

	public Object member(Object term, String identifier) {
		return true;
	}

	public Object memberDefinition(Object name, Object value) {
		return true;
	}

	public Object memberList(Object list, Object member) {
		return true;
	}

	public Object newExpression(Object term, Object arguments) {
		return true;
	}

	public Object number(String value) {
		return true;
	}

	public Object object(suneido.language.Generator.ObjectOrRecord which, Object members) {
		return true;
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		return true;
	}

	public Object postIncDec(Token incdec, Object lvalue) {
		return true;
	}

	public Object preIncDec(Token incdec, Object lvalue) {
		return true;
	}

	public Object returnStatement(Object expression) {
		return true;
	}

	public Object self() {
		return true;
	}

	public Object statementList(Object n, Object next) {
		return true;
	}

	public Object string(String value) {
		return true;
	}

	public Object subscript(Object term, Object expression) {
		return true;
	}

	public Object switchCases(Object cases, Object values, Object statements) {
		return true;
	}

	public Object switchStatement(Object expression, Object cases) {
		return true;
	}

	public Object symbol(String identifier) {
		return true;
	}

	public Object throwStatement(Object expression) {
		return true;
	}

	public Object tryStatement(Object tryStatement, Object catcher) {
		return true;
	}

	public Object unaryExpression(Token op, Object expression) {
		return true;
	}

	public Object whileStatement(Object expression, Object statement) {
		return true;
	}

}
