package suneido.language;

public abstract class Generator<T> {

	public T assignment(T term, Token op, T expression) {
		return null;
	}

	public abstract T binaryExpression(Token op, T expr1, T expr2);

	public Object andStart() {
		return null;
	}

	public abstract T and(Object label, T exprs, T expr);

	public void andEnd(Object label) {
	}

	public Object orStart() {
		return null;
	}

	public abstract T or(Object label, T exprs, T expr);

	public void orEnd(Object label) {
	}

	public Object conditionalTrue(Object label, T first) {
		return null;
	}

	public abstract T conditional(T primaryExpression, T first, T second,
			Object label);

	public void dowhileContinue(Object label) {
	}

	public T dowhileStatement(T statement, T expression, Object label) {
		return null;
	}

	public T foreverStatement(T statement, Object label) {
		return null;
	}

	public T expressionStatement(T expression) {
		return null;
	}

	public void functionBegin(String name, boolean isMethod) {
	}

	public T functionEnd(T params, T compound) {
		return null;
	}

	public abstract T identifier(String text);

	public Object ifExpr(T expr) {
		return null;
	}

	public void ifThen(Object label, T t) {
	}

	public Object ifElse(Object label) {
		return null;
	}

	public T ifStatement(T expression, T t, T e, Object label) {
		return null;
	}

	public abstract T in(T expression, T constant);

	public T returnStatement(T expression, Object context) {
		return null;
	}

	public void afterStatement(T statements) {
	}

	public T statementList(T n, T next) {
		return null;
	}

	public abstract T unaryExpression(Token op, T expression);

	public Object loop() {
		return true;
	}

	public Object dowhileLoop() {
		return true;
	}

	public void whileExpr(T expr, Object loop) {
	}

	public T whileStatement(T expr, T statement, Object loop) {
		return null;
	}

	public abstract T number(String value);

	public abstract T string(String value);

	public abstract T date(String value);

	public abstract T symbol(String identifier);

	public abstract T bool(boolean value);

	public T breakStatement(Object loop) {
		return null;
	}

	public T continueStatement(Object loop) {
		return null;
	}

	public T throwStatement(T expression) {
		return null;
	}

	public T catcher(String variable, String pattern, T statement) {
		return null;
	}

	public Object startTry() {
		return null;
	}

	public void startCatch(String var, String pattern, Object trycatch) {
	}

	public T tryStatement(T tryStatement, T catcher, Object trycatch) {
		return null;
	}

	public Object startSwitch() {
		return null;
	}

	public void startCase(Object labels) {
	}

	public void startCaseValue(Object labels) {
	}

	public void startCaseBody(Object labels) {
	}

	public T caseValues(T values, T expression, Object labels,
			boolean more) {
		return null;
	}

	public T switchCases(T cases, T values, T statements, Object labels,
			boolean moreCases) {
		return null;
	}

	public T switchStatement(T expression, T cases, Object labels) {
		return null;
	}

	public Object forInExpression(String var, T expr) {
		return true;
	}

	public T forInStatement(String var, T expr, T statement,
			Object loop) {
		return null;
	}

	public T forClassicStatement(T expr1, T expr2, T expr3,
			T statement, Object loop) {
		return null;
	}

	public Object forStart() {
		return null;
	}

	public void forIncrement(Object label) {
	}

	public void forCondition(T cond, Object loop) {
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

	/** reference */
	public T member(T term, String identifier) {
		return null;
	}

	public T subscript(T term, T expression) {
		return null;
	}

	public T functionCallTarget(T function) {
		return function;
	}

	public abstract T functionCall(T function, T arguments);

	public void newCall() {
	}

	public T newExpression(T term, T arguments) {
		return null;
	}

	public abstract T argumentList(T list, Object keyword, T expression);

	public void argumentName(Object keyword) {
	}

	public void atArgument(String n) {
	}

	public T atArgument(String n, T expr) {
		return null;
	}

	public Object blockBegin() {
		return true;
	}

	public void blockParams() {
	}

	public T blockEnd(T params, T statements) {
		return null;
	}

	public T parameters(T list, String name, T defaultValue) {
		return null;
	}

	public abstract T memberDefinition(T name, T value);

	public abstract T memberList(MType which, T list, T member);

	public void classBegin(String name) {
	}

	public T classEnd(String base, T members) {
		return null;
	}

	public void objectBegin() {
	}

	public enum MType { OBJECT, RECORD, CLASS };

	public abstract T objectEnd(MType which, T members);

	public abstract T constant(T value);

	public void addSuperInit() {
	}

	public T rvalue(T expr) {
		return null;
	}

	public T lvalueForAssign(T term, Token op) {
		return null;
	}

	public void finish() {
	}

	public T argumentListConstant(T args, Object keyword, T value) {
		return argumentList(args, keyword, constant(value));
	}

	public T superCallTarget(String method) {
		return null;
	}

	public T selfRef() {
		return null;
	}

}
