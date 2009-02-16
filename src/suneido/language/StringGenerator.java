package suneido.language;

import suneido.SuString;
import suneido.SuValue;

public class StringGenerator implements Generator {

	public String assignment(String text, Object expression) {
		return text + " = (" + (String) expression + ")";
	}

	public String conditional(Object expression, Object first, Object second) {
		return "(" + expression + " ? " + (String) first + " : " + (String) second + ")";
	}

	public String constant(SuValue result) {
		return result.toString();
	}

	public SuValue function(Object compound) {
		return SuString.valueOf(str(compound).trim());
	}

	public String identifier(String text) {
		return text;
	}

	public String ifStatement(Object expression, Object t, Object f) {
		return "if (" + expression + ") {" + str(" ", t) + " }" + str(" else { ", f, " }");
	}

	public String returnStatement(Object expression) {
		return "return" + str(" ", expression) + ";";
	}

	public Object expressionStatement(Object expression) {
		return (String) expression + ";";
	}

	public String statementList(Object n, Object next) {
		return str(n) + str(next) + " ";
	}

	public Object whileStatement(Object expression, Object statement) {
		return "while (" + expression + ") {" + str(" ", statement) + " }";
	}

	public Object dowhileStatement(Object statement, Object expression) {
		return "do {" + str(" ", statement) + " } while (" + expression + ")";
	}

	public Object binaryExpression(String op, Object list, Object next) {
		return str(list) + ' ' + op + ' ' + str(next);
	}

	public Object unaryExpression(String op, Object expression) {
		return op + (String) expression;
	}

	private String str(Object x) {
		return x == null ? "" : (String) x;
	}
	private String str(String s, Object x) {
		return x == null ? "" : s + (String) x;
	}
	private String str(String s, Object x, String t) {
		return x == null ? "" : s + (String) x + t;
	}

}
