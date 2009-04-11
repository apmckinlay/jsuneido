package suneido.language;

import suneido.language.ParseExpression.Value;

public interface Generator<T> {

	void lvalue(Value<T> value);

	T assignment(T term, Value<T> value, Token op, T expression);

	T binaryExpression(Token op, T expr1, T expr2);

	T and(T expr1, T expr2);
	Object and(Object prevlabel);
	void andEnd(Object label);

	T or(T expr1, T expr2);
	Object or(Object label);
	void orEnd(Object label);

	Object conditionalTrue(Object label, T first);
	T conditional(T primaryExpression, T first, T second, Object label);

	T dowhileStatement(T statement, T expression, Object label);

	T foreverStatement(T statement, Object label);

	T expressionStatement(T expression);

	T function(T params, T compound);

	enum FuncOrBlock { FUNC, BLOCK };
	Object startFunction(FuncOrBlock funcOrBlock);

	T identifier(String text);

	Object ifExpr(T expr);
	void ifThen(Object label, T t);
	Object ifElse(Object label);
	T ifStatement(T expression, T t, T e, Object label);

	T in(T expression, T constant);

	T returnStatement(T expression, Object context);

	void afterStatement(T statements);

	T statementList(T n, T next);

	T unaryExpression(Token op, T expression);

	Object loop();
	void whileExpr(T expr, Object loop);

	T whileStatement(T expr, T statement, Object loop);

	T number(String value);

	T string(String value);

	T date(String value);

	T symbol(String identifier);

	T bool(boolean value);

	T breakStatement(Object loop);

	T continueStatement(Object loop);

	T throwStatement(T expression);

	T catcher(String variable, String pattern, T statement);

	T tryStatement(T tryStatement, T catcher);

	Object startSwitch();
	void startCase(Object labels);
	void startCaseValue();
	void startCaseBody(Object labels);
	T caseValues(T values, T expression, Object labels, boolean more);
	T switchCases(T cases, T values, T statements, Object labels);
	T switchStatement(T expression, T cases, Object labels);

	Object forInExpression(String var, T expr);
	T forInStatement(String var, T expr, T statement, Object loop);

	T forClassicStatement(T expr1, T expr2, T expr3, T statement, Object loop);
	Object forStart();
	void forIncrement(Object label);
	void forCondition(T cond, Object loop);

	T expressionList(T list, T expression);

	T preIncDec(T term, Token incdec, Value<T> value);

	T postIncDec(T term, Token incdec, Value<T> value);

	T member(T term, String identifier);

	T subscript(T term, T expression);

	T self();

	void preFunctionCall(Value<T> value);
	T functionCall(T function, Value<T> value, T arguments);

	void newCall();
	T newExpression(T term, T arguments);

	T argumentList(T list, String keyword, T expression);

	void argumentName(String keyword);

	void atArgument(String n);
	T atArgument(String n, T expr);

	void blockParams();
	T block(T params, T statements);

	T parameters(T list, String name, T defaultValue);

	T memberList(ObjectOrRecord which, T list, T member);

	T classConstant(String base, T members);

	T memberDefinition(T name, T value);

	enum ObjectOrRecord { OBJECT, RECORD };
	T object(ObjectOrRecord which, T members);

	T constant(T value);

}
