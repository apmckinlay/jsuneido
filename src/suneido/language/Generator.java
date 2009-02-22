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

	T breakStatement();

	T continueStatement();

	T throwStatement(T expression);

	T catcher(String variable, String pattern, T statement);

	T tryStatement(T tryStatement, T catcher);

	T caseValues(T values, T expression);

	T switchCases(T cases, T values, T statements);

	T switchStatement(T expression, T cases);

	T forInStatement(String var, T expr, T statement);

	T forClassicStatement(T expr1, T expr2, T expr3, T statement);

	T expressionList(T list, T expression);

}
