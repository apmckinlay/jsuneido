package suneido.language;

public class TestGenerator extends Generator<Object> {

	private void print(Object... args) {
		for (Object arg : args)
			if (arg != null)
				System.out.print(arg + " ");
		System.out.println();
	}

	@Override
	public Object and(Object label, Object expr1, Object expr2) {
		print("and(expr, expr)", expr1, expr2);
		return null;
	}

	@Override
	public Object andStart() {
		print("andStart");
		return "label";
	}

	@Override
	public Object andEnd(Object label, Object exprs) {
		print("andEnd");
		return "and-result";
	}

	@Override
	public Object argumentList(Object list, Object keyword, Object expression) {
		print("argumentList list=", list, "keyword=", keyword, expression);
		return "argumentList-result";
	}

	@Override
	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		print("binaryExpression", op, expr1, expr2);
		return null;
	}

	@Override
	public Object bool(boolean value) {
		print("bool", value);
		return null;
	}

	@Override
	public Object conditional(Object primaryExpression, Object first,
			Object second, Object label) {
		print("conditional", primaryExpression, first, second, label);
		return null;
	}

	@Override
	public Object constant(Object value) {
		print("constant", value);
		return "constant=" + value;
	}

	@Override
	public Object date(String value) {
		print("date", value);
		return null;
	}

	@Override
	public Object functionCall(Object function, Object arguments) {
		print("functionCall", function, arguments);
		return null;
	}

	@Override
	public Object identifier(String text) {
		print("identifier", text);
		return "identifier=" + text;
	}

	@Override
	public Object in(Object expression, Object constant) {
		print("in", expression, constant);
		return null;
	}

	@Override
	public Object memberDefinition(Object name, Object value) {
		print("memberDefinition", name, value);
		return null;
	}

	@Override
	public Object memberList(suneido.language.Generator.MType which,
			Object list, Object member) {
		print("memberList", which, list, member);
		return null;
	}

	@Override
	public Object number(String value) {
		print("number", value);
		return "number=" + value;
	}

	@Override
	public Object objectEnd(suneido.language.Generator.MType which, Object members) {
		print("object", which, members);
		return null;
	}

	@Override
	public Object orStart() {
		print("orStart");
		return null;
	}

	@Override
	public Object or(Object label, Object expr1, Object expr2) {
		print("or", expr1, expr2);
		return null;
	}

	@Override
	public Object orEnd(Object label, Object exprs) {
		print("orEnd", exprs);
		return "or-result";
	}

//	@Override
//	public Object rvalue(Object expr) {
//		print("rvalue", expr);
//		return "rvalue-result";
//	}

	@Override
	public Object string(String value) {
		print("string", value);
		return value;
	}

	@Override
	public Object symbol(String identifier) {
		print("symbol", identifier);
		return identifier;
	}

	@Override
	public Object unaryExpression(Token op, Object expression) {
		print("unaryExpression", op, expression);
		return null;
	}

	@Override
	public Object assignment(Object term, Token op, Object expression) {
		print("assignment", term, op, expression);
		return "assignment-result";
	}

	@Override
	public Object lvalueForAssign(Object term, Token op) {
		print("lvalueForAssign", term, op);
		return term;
	}

	@Override
	public void classBegin(String name) {
		print("classBegin", name);
	}

	@Override
	public Object classEnd(String base, Object members) {
		print("classEnd", base, members);
		return "classEnd-result";
	}

	@Override
	public void afterStatement(Object statements) {
		print("afterStatement", statements);
	}

	@Override
	public void functionBegin(Object name, boolean isMethod) {
		print("functionBegin", name, isMethod ? "isMethod" : null);
	}

	@Override
	public Object functionEnd(Object params, Object compound, boolean isMethod) {
		print("functionEnd", params, compound);
		return "functionEnd-result";
	}

	@Override
	public Object parameters(Object list, String name, Object defaultValue) {
		print("parameters", name, defaultValue);
		return "parameters-result";
	}

	@Override
	public Object statementList(Object list, Object next) {
		print("statementList", next);
		return "statementList";
	}

	@Override
	public Object functionCallTarget(Object function) {
		print("functionCallTarget", function);
		return "functionCallTarget-result";
	}

	@Override
	public Object member(Object term, String identifier) {
		print("member", term, identifier);
		return "member-" + identifier;
	}

	@Override
	public Object returnStatement(Object expression, Object context) {
		print("returnStatement", expression, context);
		return "returnStatement-result";
	}

	@Override
	public Object forInExpression(String var, Object expr) {
		print("forInExpression", var, expr);
		return "forInExpression-result";
	}

	@Override
	public Object forInStatement(String var, Object expr, Object statement,
			Object loop) {
		print("forInStatement", var, expr, statement, loop);
		return "forInStatement-result";
	}

	@Override
	public Object ifExpr(Object expr) {
		print("ifExpr", expr);
		return "ifExpr-result";
	}

	@Override
	public Object conditionalTrue(Object label, Object first) {
		print("conditionalTrue", label, first);
		return "conditionalTrue(" + label + ", " + first + ")";
	}

	@Override
	public Object switchStatement(Object expression, Object cases, Object labels) {
		print("switchStatement");
		return null;
	}

	@Override
	public Object switchCases(Object cases, Object values, Object statements,
			Object labels, boolean moreCases) {
		print("switchCases " + (moreCases ? "moreCases" : ""));
		return null;
	}

	@Override
	public void startCase(Object labels) {
		print("startCase");
	}

	@Override
	public void startCaseBody(Object labels) {
		print("startCaseBody");
	}

	@Override
	public void startCaseValue(Object labels) {
		print("startCaseValue");
	}

	@Override
	public Object startSwitch() {
		print("startSwitch");
		return null;
	}

	@Override
	public Object caseValues(Object values, Object expression, Object labels,
			boolean more) {
		print("caseValues");
		return null;
	}

	@Override
	public Object startTry() {
		print("startTry");
		return null;
	}

	@Override
	public void startCatch(String var, String pattern, Object trycatch) {
		print("startCatch", var, pattern, trycatch);
		super.startCatch(var, pattern, trycatch);
	}

	@Override
	public Object catcher(String variable, String pattern, Object statement) {
		print("catcher", variable, pattern, statement);
		return null;
	}

	@Override
	public Object tryStatement(Object tryStatement, Object catcher,
			Object trycatch) {
		print("tryStatement", tryStatement, catcher, trycatch);
		return null;
	}

	@Override
	public Object selfRef() {
		print("selfRef");
		return "selfRef-result";
	}

	@Override
	public Object rvalue(Object expr) {
		print("rvalue", expr);
		return "rvalue-" + expr;
	}

	public static void main(String[] args) {
		String s = "class { M() { } }";
		Lexer lexer = new Lexer(s);
		TestGenerator generator = new TestGenerator();
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		pc.parse();
	}

}
