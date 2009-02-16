package suneido.language;

import suneido.SuValue;

public interface Builder {

	Object assignment(String text, Object expression);

	Object binaryExpression(String op, Object list, Object next);

	Object conditional(Object primaryExpression, Object first, Object second);

	Object constant(SuValue result);

	Object dowhileStatement(Object statement, Object expression);

	Object expressionStatement(Object expression);

	SuValue function(Object compound);

	Object identifier(String text);

	Object ifStatement(Object expression, Object t, Object f);

	Object returnStatement(Object expression);

	Object statementList(Object n, Object next);

	Object unaryExpression(String op, Object expression);

	Object whileStatement(Object expression, Object statement);

}
