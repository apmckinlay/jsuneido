package suneido.language;

import suneido.SuValue;

public interface Builder {

	SuValue function(Object compound);

	Object assignment(String text, Object expression);

	Object conditional(Object primaryExpression, Object first, Object second);

	Object constant(SuValue result);

	Object identifier(String text);

	Object ifStatement(Object expression, Object t, Object f);

	Object returnStatement(Object expression);

	Object statementList(Object n, Object next);

	Object expressionStatement(Object expression);

}
