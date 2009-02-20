package suneido.language;

import suneido.SuValue;

public interface Generator<T> {

	T assignment(String text, T expression);

	T binaryExpression(Token op, T list, T next);

	T conditional(T primaryExpression, T first, T second);

	T constant(SuValue result);

	T dowhileStatement(T statement, T expression);

	T foreverStatement(T statement);

	T expressionStatement(T expression);

	T function(T compound);

	T identifier(String text);

	T ifStatement(T expression, T t, T f);

	T returnStatement(T expression);

	T statementList(T n, T next);

	T unaryExpression(Token op, T expression);

	T whileStatement(T expression, T statement);

	T number(String value);

	T string(String value);

	T date(String value);

	T symbol(String value);

	T bool(String value);

}
