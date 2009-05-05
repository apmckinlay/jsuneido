package suneido.language;

import suneido.language.ParseExpression.Value;

public abstract class Generator<T> {

	public abstract void lvalue(Value<T> value);

	public abstract void lvalueForAssign(Value<T> value, Token op);

	public abstract T assignment(T term, Value<T> value, Token op, T expression);

	public abstract T binaryExpression(Token op, T expr1, T expr2);

	public abstract T and(T expr1, T expr2);

	public abstract Object and(Object prevlabel);

	public abstract void andEnd(Object label);

	public abstract T or(T expr1, T expr2);

	public abstract Object or(Object label);

	public abstract void orEnd(Object label);

	public abstract Object conditionalTrue(Object label, T first);

	public abstract T conditional(T primaryExpression, T first, T second,
			Object label);

	public abstract T dowhileStatement(T statement, T expression, Object label);

	public abstract T foreverStatement(T statement, Object label);

	public abstract T expressionStatement(T expression);

	public abstract T function(T params, T compound);

	public abstract void startFunction(T name);

	public abstract Object startBlock();

	public abstract T identifier(String text);

	public abstract Object ifExpr(T expr);

	public abstract void ifThen(Object label, T t);

	public abstract Object ifElse(Object label);

	public abstract T ifStatement(T expression, T t, T e, Object label);

	public abstract T in(T expression, T constant);

	public abstract T returnStatement(T expression, Object context);

	public abstract void afterStatement(T statements);

	public abstract T statementList(T n, T next);

	public abstract T unaryExpression(Token op, T expression);

	public abstract Object loop();

	public abstract void whileExpr(T expr, Object loop);

	public abstract T whileStatement(T expr, T statement, Object loop);

	public abstract T number(String value);

	public abstract T string(String value);

	public abstract T date(String value);

	public abstract T symbol(String identifier);

	public abstract T bool(boolean value);

	public abstract T breakStatement(Object loop);

	public abstract T continueStatement(Object loop);

	public abstract T throwStatement(T expression);

	public abstract T catcher(String variable, String pattern, T statement);

	public abstract Object startTry();

	public abstract void startCatch(String var, String pattern, Object trycatch);

	public abstract T tryStatement(T tryStatement, T catcher, Object trycatch);

	public abstract Object startSwitch();

	public abstract void startCase(Object labels);

	public abstract void startCaseValue();

	public abstract void startCaseBody(Object labels);

	public abstract T caseValues(T values, T expression, Object labels,
			boolean more);

	public abstract T switchCases(T cases, T values, T statements, Object labels);

	public abstract T switchStatement(T expression, T cases, Object labels);

	public abstract Object forInExpression(String var, T expr);

	public abstract T forInStatement(String var, T expr, T statement,
			Object loop);

	public abstract T forClassicStatement(T expr1, T expr2, T expr3,
			T statement,
			Object loop);

	public abstract Object forStart();

	public abstract void forIncrement(Object label);

	public abstract void forCondition(T cond, Object loop);

	public abstract T expressionList(T list, T expression);

	public abstract T preIncDec(T term, Token incdec, Value<T> value);

	public abstract T postIncDec(T term, Token incdec, Value<T> value);

	public abstract T member(T term, Value<T> value);

	public abstract T subscript(T term, T expression);

	public abstract T selfRef();

	public abstract T superRef();

	public abstract void preFunctionCall(Value<T> value);

	public abstract T functionCall(T function, Value<T> value, T arguments);

	public abstract void newCall();

	public abstract T newExpression(T term, T arguments);

	public abstract T argumentList(T list, String keyword, T expression);

	public abstract void argumentName(String keyword);

	public abstract void atArgument(String n);

	public abstract T atArgument(String n, T expr);

	public abstract void blockParams();

	public abstract T block(T params, T statements);

	public abstract T parameters(T list, String name, T defaultValue);

	public abstract T memberList(MType which, T list, T member);

	public abstract void startClass();

	public abstract T classConstant(String base, T members);

	public abstract T memberDefinition(T name, T value);

	public enum MType {
		OBJECT, RECORD, CLASS
	};

	public abstract T object(MType which, T members);

	public abstract T constant(T value);

	public abstract void addSuperInit();

	public abstract T rvalue(T expr);

	public abstract void finish();

	public void startObject() {
	}

}
