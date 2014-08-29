/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.jsdi.DllInterface;

public abstract class Generator<T> {

	public T assignment(T term, Token op, T expression) {
		return null;
	}

	public abstract T binaryExpression(Token op, T expr1, T expr2);

	public abstract T and(T exprs, T expr);

	public T andEnd(T exprs) {
		return exprs;
	}

	public abstract T or(T exprs, T expr);

	public T orEnd(T exprs) {
		return exprs;
	}

	public abstract T conditional(T primaryExpression, T first, T second);

	public T dowhileStatement(T statement, T expression) {
		return null;
	}

	public T foreverStatement(T statement) {
		return null;
	}

	public T function(T params, T compound, boolean isMethod, int lineNumber) {
		return null;
	}

	public T identifier(String text, int lineNumber) {
		return identifier(text);
	}

	public abstract T identifier(String text);

	public T ifStatement(T expression, T t, T e) {
		return null;
	}

	public abstract T in(T expression, T list);

	public T returnStatement(T expression, Object context, int lineNumber) {
		return null;
	}

	public T statementList(T list, T next) {
		return null;
	}

	public abstract T unaryExpression(Token op, T expression);

	public T whileStatement(T expr, T statement) {
		return null;
	}

	public T number(String value, int lineNumber) {
		return number(value);
	}

	public abstract T number(String value);

	public T string(String value, int lineNumber) {
		return string(value);
	}

	public abstract T string(String value);

	public abstract T date(String value, int lineNumber);

	public abstract T symbol(String identifier, int lineNumber);

	public abstract T bool(boolean value, int lineNumber); // Actual boolean literal in source

	public T boolTrue() { // Synthetic true literal inserted by the compiler
		return bool(true, -1);
	}

	public T breakStatement(int lineNumber) {
		return null;
	}

	public T continueStatement(int lineNumber) {
		return null;
	}

	public T throwStatement(T expression, int lineNumber) {
		return null;
	}

	public T catcher(String variable, String pattern, T statement) {
		return null;
	}

	public T tryStatement(T tryStatement, T catcher) {
		return null;
	}

	public T caseValues(T values, T expression) {
		return null;
	}

	public T switchCases(T cases, T values, T statements) {
		return null;
	}

	public T switchStatement(T expression, T cases) {
		return null;
	}

	public T forInStatement(String var, T expr, T statement) {
		return null;
	}

	public T forClassicStatement(T expr1, T expr2, T expr3, T statement) {
		return null;
	}

	public T expressionList(T list, T expression) {
		return null;
	}

	public T preIncDec(T term, Token incdec) {
		return null;
	}

	public T postIncDec(T term, Token incdec) {
		return null;
	}

	public T memberRef(T term, String identifier, int lineNumber) {
		return null;
	}

	public T subscript(T term, T expression) {
		return null;
	}

	public abstract T functionCall(T function, T arguments);

	public T newExpression(T term, T arguments) {
		return null;
	}

	public abstract T argumentList(T list, T keyword, T expression);

	public void atArgument(String n) {
	}

	public T atArgument(String n, T expr) {
		return null;
	}

	public T block(T params, T statements, int lineNumber) {
		return null;
	}

	public T parameters(T list, String name, T defaultValue) {
		return null;
	}

	public abstract T memberDefinition(T name, T value);

	public abstract T memberList(MType which, T list, T member);

	public T clazz(String base, T members, int lineNumber) {
		return null;
	}

	public enum MType { OBJECT, RECORD };

	public abstract T object(MType which, T members, int lineNumber);

	public T rvalue(T expr) {
		return expr;
	}

	public T constant(T expr) {
		return expr;
	}

	public T superCallTarget(String method) {
		return null;
	}

	public T selfRef(int lineNumber) {
		return null;
	}

	public T range(Token type, T expr1, T expr2) {
		return null;
	}

	//
	// DLL INTERFACE PARSING...
	//

	@DllInterface
	public T struct(T structMembers, int lineNumber) {
		return null;
	}

	@DllInterface
	public T dll(String libraryName, String userFunctionName, String returnType,
			T dllParams, int lineNumber) {
		return null;
	}

	@DllInterface
	public T callback(T dllParams, int lineNumber) {
		return null;
	}

	@DllInterface
	public T typeList(T list, String name, boolean inTag, String baseType,
			Token storageType, String numElems) {
		return null;
	}
}
