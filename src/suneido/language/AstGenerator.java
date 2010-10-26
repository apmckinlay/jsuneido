package suneido.language;

import java.util.ArrayList;


public class AstGenerator extends Generator<AstNode> {
	private final static AstNode NIL_STATEMENT = new AstNode(Token.NIL);
	private final static AstNode EMPTY_LIST = new AstNode(Token.LIST);
	private final static AstNode EMPTY_COMPOUND =
		new AstNode(Token.LIST, NIL_STATEMENT);
	public static final char METHOD_SEPARATOR = '\u00A3';
	private final String globalName;
	private String curName;

	public AstGenerator(String name) {
		this.globalName = name;
	}

	@Override
	public void classBegin(String memberName) {
		nameBegin(memberName, "$c");
	}

	@Override
	public AstNode classEnd(String base, AstNode members) {
		AstNode baseAst = base == null ? null : new AstNode(Token.STRING, base);
		if (members == null)
			members = EMPTY_LIST;
		AstNode ast = new AstNode(Token.CLASS, curName, baseAst, members);
		nameEnd();
		return ast;
	}

	@Override
	public void functionBegin(AstNode member, boolean isMethod) {
		nameBegin(member == null ? null : member.value,
				isMethod ? METHOD_SEPARATOR + "f" : "$f");
	}

	@Override
	public AstNode functionEnd(AstNode params, AstNode body, boolean isMethod) {
		if (params == null)
			params = EMPTY_LIST;
		if (body == null)
			body = EMPTY_COMPOUND;
		AstNode f = new AstNode(isMethod ? Token.METHOD : Token.FUNCTION,
				curName, params, body);
		nameEnd();
		return f;
	}

	@Override
	public Object blockBegin() {
		nameBegin(null, "$b");
		return true; // non-null to tell ParseFunction we're inside block
	}

	@Override
	public AstNode blockEnd(AstNode params, AstNode body) {
		if (params == null)
			params = EMPTY_LIST;
		if (body == null)
			body = EMPTY_COMPOUND;
		AstNode block = new AstNode(Token.BLOCK, curName, params, body);
		nameEnd();
		return block;
	}

	private void nameBegin(String memberName, String def) {
		if (curName == null)
			curName = javify(globalName);
		else if (memberName != null)
			curName += def.substring(0, 1) + javify(memberName);
		else
			curName += def;
	}

	public static String javify(String name) {
		return name.replace('?', 'Q').replace('!', 'X');
	}

	private void nameEnd() {
		int i = Math.max(curName.lastIndexOf('$'), curName.lastIndexOf(METHOD_SEPARATOR));
		curName = i == -1 ? "" : curName.substring(0, i);
	}

	@Override
	public AstNode parameters(AstNode list, String name, AstNode defaultValue) {
		return list(list, new AstNode(Token.IDENTIFIER, name, defaultValue));
	}

	@Override
	public AstNode memberList(MType which, AstNode list, AstNode memdef) {
		return list(list, memdef);
	}

	@Override
	public AstNode memberDefinition(AstNode name, AstNode value) {
		return new AstNode(Token.MEMBER, name, value);
	}

	@Override
	public AstNode objectEnd(MType which, AstNode members) {
		if (members == null)
			members = EMPTY_LIST;
		return new AstNode(Token.valueOf(which.toString()), members.children);
	}

	// statements

	@Override
	public AstNode statementList(AstNode list, AstNode next) {
		return list(list, next == null ? NIL_STATEMENT : next);
	}

	@Override
	public AstNode dowhileStatement(AstNode statement, AstNode expr, Object label) {
		return new AstNode(Token.DO, statement, expr);
	}

	@Override
	public AstNode whileStatement(AstNode expr, AstNode statement, Object loop) {
		return new AstNode(Token.WHILE, expr, statement);
	}

	@Override
	public AstNode ifStatement(AstNode expr, AstNode t, AstNode e, Object label) {
		return new AstNode(Token.IF, expr, t, e);
	}

	@Override
	public AstNode foreverStatement(AstNode statement, Object label) {
		return new AstNode(Token.FOREVER, statement);
	}

	@Override
	public AstNode forClassicStatement(AstNode expr1, AstNode expr2,
			AstNode expr3, AstNode statement, Object loop) {
		return new AstNode(Token.FOR, expr1, expr2, expr3, statement);
	}

	@Override
	public AstNode forInStatement(String var, AstNode expr, AstNode statement,
			Object loop) {
		return new AstNode(Token.FOR_IN, var, expr, statement);
	}

	@Override
	public AstNode returnStatement(AstNode expr, Object context) {
		return new AstNode(Token.RETURN, expr);
	}

	@Override
	public AstNode breakStatement(Object loop) {
		return new AstNode(Token.BREAK);
	}

	@Override
	public AstNode continueStatement(Object loop) {
		return new AstNode(Token.CONTINUE);
	}

	@Override
	public AstNode throwStatement(AstNode expr) {
		return new AstNode(Token.THROW, expr);
	}

	@Override
	public AstNode tryStatement(AstNode tryStatement, AstNode catcher,
			Object trycatch) {
		return new AstNode(Token.TRY, tryStatement, catcher);
	}

	@Override
	public AstNode catcher(String variable, String pattern, AstNode statement) {
		AstNode p = pattern == null ? null : new AstNode(Token.STRING, pattern);
		if (statement == null)
			statement = EMPTY_LIST;
		return new AstNode(Token.CATCH, variable, p, statement);
	}

	@Override
	public AstNode switchStatement(AstNode expr, AstNode cases, Object labels) {
		if (cases == null)
			cases = EMPTY_LIST;
		return new AstNode(Token.SWITCH, expr, cases);
	}

	@Override
	public AstNode switchCases(AstNode cases, AstNode values,
			AstNode statements, Object labels, boolean moreCases) {
		if (values == null)
			values = EMPTY_LIST;
		return list(cases, new AstNode(Token.CASE, values, statements));
	}

	@Override
	public AstNode caseValues(AstNode values, AstNode expr,
			Object labels, boolean more) {
		return list(values, expr);
	}

	@Override
	public AstNode expressionStatement(AstNode expr) {
		return expr;
	}

	@Override
	public AstNode binaryExpression(Token op, AstNode expr1, AstNode expr2) {
		return new AstNode(Token.BINARYOP, new AstNode(op), expr1, expr2);
	}

	@Override
	public AstNode and(Object label, AstNode list, AstNode expr) {
		return list(list, expr);
	}

	@Override
	public AstNode andEnd(Object label, AstNode exprs) {
		return new AstNode(Token.AND, exprs.children);
	}

	@Override
	public AstNode or(Object label, AstNode list, AstNode expr) {
		return list(list, expr);
	}

	@Override
	public AstNode orEnd(Object label, AstNode exprs) {
		return new AstNode(Token.OR, exprs.children);
	}

	@Override
	public AstNode conditional(AstNode expr, AstNode first, AstNode second, Object label) {
		return new AstNode(Token.Q_MARK, expr, first, second);
	}

	@Override
	public AstNode identifier(String text) {
		return new AstNode(Token.IDENTIFIER, text);
	}

	@Override
	public AstNode in(AstNode expr, AstNode list) {
		return new AstNode(Token.IN, expr, list);
	}

	@Override
	public AstNode unaryExpression(Token op, AstNode expr) {
		return new AstNode(op, expr);
	}

	@Override
	public AstNode number(String value) {
		return new AstNode(Token.NUMBER, value);
	}

	@Override
	public AstNode string(String value) {
		return new AstNode(Token.STRING, value);
	}

	@Override
	public AstNode date(String value) {
		return new AstNode(Token.DATE, value);
	}

	@Override
	public AstNode symbol(String identifier) {
		return new AstNode(Token.SYMBOL, identifier);
	}

	@Override
	public AstNode bool(boolean value) {
		return value ? TRUE : FALSE;
	}
	private static final AstNode TRUE = new AstNode(Token.TRUE);
	private static final AstNode FALSE = new AstNode(Token.FALSE);

	@Override
	public AstNode superCallTarget(String method) {
		return new AstNode(Token.SUPER, method);
	}

	@Override
	public AstNode functionCall(AstNode function, AstNode arguments) {
		if (arguments == null)
			arguments = EMPTY_LIST;
		return new AstNode(Token.CALL, function, arguments);
	}

	@Override
	public AstNode argumentList(AstNode list, Object keyword, AstNode expr) {
		Object argName = null;
		if (keyword != null) {
			AstNode argNameAst = (AstNode) keyword;
			argName = (argNameAst.token == Token.NUMBER)
				? Ops.stringToNumber(argNameAst.value) : argNameAst.value;
		}
		return list(list, AstNode.of(Token.ARG, argName, expr));
	}

	@Override
	public AstNode atArgument(String n, AstNode expr) {
		assert "0".equals(n) || "1".equals(n);
		return new AstNode(Token.AT, n, expr);
	}

	@Override
	public AstNode newExpression(AstNode target, AstNode arguments) {
		if (arguments == null)
			arguments = EMPTY_LIST;
		return new AstNode(Token.NEW, target, arguments);
	}

	@Override
	public AstNode assignment(AstNode term, Token op, AstNode expr) {
		return op == Token.EQ
				? new AstNode(op, term, expr)
				: new AstNode(Token.ASSIGNOP, new AstNode(op), term, expr);
	}

	@Override
	public AstNode preIncDec(AstNode term, Token incdec) {
		return new AstNode(Token.PREINCDEC, new AstNode(incdec), term);
	}

	@Override
	public AstNode postIncDec(AstNode term, Token incdec) {
		return new AstNode(Token.POSTINCDEC, new AstNode(incdec), term);
	}

	@Override
	public AstNode expressionList(AstNode list, AstNode expr) {
		return list(list, expr);
	}

	@Override
	public AstNode selfRef() {
		return new AstNode(Token.SELFREF);
	}

	@Override
	public AstNode rvalue(AstNode expr) {
		// used to differentiate X.F() from (X.F)()
		return new AstNode(Token.RVALUE, expr);
	}

	@Override
	public AstNode member(AstNode term, String identifier) {
		return new AstNode(Token.MEMBER, identifier, term);
	}

	@Override
	public AstNode subscript(AstNode term, AstNode expression) {
		return new AstNode(Token.SUBSCRIPT, term, expression);
	}

	private AstNode list(AstNode list, AstNode next) {
		if (list == null)
			list = new AstNode(Token.LIST, new ArrayList<AstNode>());
		list.children.add(next);
		return list;
	}

	public static void main(String[] args) {
		String s = "class : B { M: 1 }";
		Lexer lexer = new Lexer(s);
		AstGenerator generator = new AstGenerator("Test");
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		System.out.println(ast);
	}

}
