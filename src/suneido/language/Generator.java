package suneido.language;

import suneido.SuValue;

public interface Generator<T> {

	T assignment(T term, Token op, T expression);

	T binaryExpression(Token op, T list, T next);

	T conditional(T primaryExpression, T first, T second);

	T constant(SuValue result);

	T dowhileStatement(T statement, T expression);

	T foreverStatement(T statement);

	T expressionStatement(T expression);

	T function(T params, T compound);

	T identifier(String text);

	T ifStatement(T expression, T t, T f);

	T returnStatement(T expression);

	T statementList(T n, T next);

	T unaryExpression(Token op, T expression);

	T whileStatement(T expression, T statement);

	T number(String value);

	T string(String value);

	T date(String value);

	T symbol(String identifier);

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

	T preIncDec(Token incdec, T lvalue);

	T postIncDec(Token incdec, T lvalue);

	T member(T term, String identifier);

	T subscript(T term, T expression);

	T self();

	T functionCall(T function, T arguments);

	T newExpression(T term, T arguments);

	T argumentList(T list, String keyword, T expression);

	T atArgument(String n, T expr);

	T block(T params, T statements);

	T parameters(T list, String name, T defaultValue);

	T memberList(T list, T member);

	T classConstant(String base, T members);

	T memberDefinition(T name, T value);

	enum ObjectOrRecord { OBJECT, RECORD };

	T object(ObjectOrRecord which, T members);

	// for queries

	T drop(String name);

	T rename(String from, String to);

	T view(String name, String definition);

	T sview(String name, String definition);

	T create(String table, T schema);

	T ensure(String table, T schema);

	T columns(T columns, String column);

	T indexes(T indexes, T index);

	T schema(T columns, T indexes);

	T index(boolean key, boolean unique, boolean lower, T columns, T foreignKey);

	T foreignKey(String table, T columns, Token mode);

	T alterCreate(String table, T schema);

	T alterDrop(String table, T schema);

	T alterRename(String table, T renames);

	T renames(T renames, String from, String to);

}
