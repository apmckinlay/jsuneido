/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.ArrayList;

import suneido.jsdi.DllInterface;

public class AstGenerator extends Generator<AstNode> {
	private static final AstNode NIL_STATEMENT = new AstNode(Token.NIL);
	private static final AstNode EMPTY_LIST = new AstNode(Token.LIST);
	private static final AstNode EMPTY_COMPOUND =
		new AstNode(Token.LIST, NIL_STATEMENT);

	@Override
	public AstNode clazz(String base, AstNode members) {
		AstNode baseAst = base == null ? null : new AstNode(Token.STRING, base);
		if (members == null)
			members = EMPTY_LIST;
		return new AstNode(Token.CLASS, baseAst, members);
	}

	@Override
	public AstNode function(AstNode params, AstNode body, boolean isMethod) {
		if (params == null)
			params = EMPTY_LIST;
		if (body == null)
			body = EMPTY_COMPOUND;
		return new AstNode(isMethod ? Token.METHOD : Token.FUNCTION,
				params, body);
	}

	private static final AstNode itParams = new AstNode(Token.LIST,
			new AstNode(Token.IDENTIFIER, "it", (AstNode) null));

	@Override
	public AstNode block(AstNode params, AstNode body) {
		if (params == null)
			params = EMPTY_LIST;
		if (body == null)
			body = EMPTY_COMPOUND;
		if (params.children.isEmpty() && AstBlockIt.check(body))
			params = itParams;
		// extra child is used by AstSharesVars to mark closures
		return new AstNode(Token.BLOCK, params, body, null);
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
	public AstNode object(MType which, AstNode members) {
		if (members == null)
			members = EMPTY_LIST;
		return new AstNode(Token.valueOf(which.toString()), members.children);
	}

	@Override
	public AstNode statementList(AstNode list, AstNode next) {
		return list(list, next == null ? NIL_STATEMENT : next);
	}

	@Override
	public AstNode dowhileStatement(AstNode statement, AstNode expr) {
		return new AstNode(Token.DO, statement, expr);
	}

	@Override
	public AstNode whileStatement(AstNode expr, AstNode statement) {
		return new AstNode(Token.WHILE, expr, statement);
	}

	@Override
	public AstNode ifStatement(AstNode expr, AstNode t, AstNode e) {
		return new AstNode(Token.IF, expr, t, e);
	}

	@Override
	public AstNode foreverStatement(AstNode statement) {
		return new AstNode(Token.FOREVER, statement);
	}

	@Override
	public AstNode forClassicStatement(AstNode expr1, AstNode expr2,
			AstNode expr3, AstNode statement) {
		return new AstNode(Token.FOR, expr1, expr2, expr3, statement);
	}

	@Override
	public AstNode forInStatement(String var, AstNode expr, AstNode statement) {
		return new AstNode(Token.FOR_IN, var, expr, statement);
	}

	@Override
	public AstNode returnStatement(AstNode expr, Object context) {
		return new AstNode(Token.RETURN, expr);
	}

	@Override
	public AstNode breakStatement() {
		return new AstNode(Token.BREAK);
	}

	@Override
	public AstNode continueStatement() {
		return new AstNode(Token.CONTINUE);
	}

	@Override
	public AstNode throwStatement(AstNode expr) {
		return new AstNode(Token.THROW, expr);
	}

	@Override
	public AstNode tryStatement(AstNode tryStatement, AstNode catcher) {
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
	public AstNode switchStatement(AstNode expr, AstNode cases) {
		if (cases == null)
			cases = EMPTY_LIST;
		return new AstNode(Token.SWITCH, expr, cases);
	}

	@Override
	public AstNode switchCases(AstNode cases, AstNode values, AstNode statements) {
		if (values == null)
			values = EMPTY_LIST;
		return list(cases, new AstNode(Token.CASE, values, statements));
	}

	@Override
	public AstNode caseValues(AstNode values, AstNode expr) {
		return list(values, expr);
	}

	@Override
	public AstNode binaryExpression(Token op, AstNode expr1, AstNode expr2) {
		return new AstNode(Token.BINARYOP, new AstNode(op), expr1, expr2);
	}

	@Override
	public AstNode and(AstNode list, AstNode expr) {
		return list(list, expr);
	}

	@Override
	public AstNode andEnd(AstNode exprs) {
		return new AstNode(Token.AND, exprs.children);
	}

	@Override
	public AstNode or(AstNode list, AstNode expr) {
		return list(list, expr);
	}

	@Override
	public AstNode orEnd(AstNode exprs) {
		return new AstNode(Token.OR, exprs.children);
	}

	@Override
	public AstNode conditional(AstNode expr, AstNode first, AstNode second) {
		return new AstNode(Token.Q_MARK, expr, first, second);
	}

	@Override
	public AstNode identifier(String text) {
		return new AstNode(Token.IDENTIFIER, "_".equals(text) ? "it" : text);
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
	public AstNode argumentList(AstNode list, AstNode keyword, AstNode expr) {
		return list(list, new AstNode(Token.ARG, keyword, expr));
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
	public AstNode memberRef(AstNode term, String identifier) {
		return new AstNode(Token.MEMBER, identifier, term);
	}

	@Override
	public AstNode subscript(AstNode term, AstNode expression) {
		return new AstNode(Token.SUBSCRIPT, term, expression);
	}

	@Override
	public AstNode range(Token type, AstNode from, AstNode to) {
		return new AstNode(type, from, to);
	}

	@Override @DllInterface
	public AstNode struct(AstNode structMembers) {
		if (null == structMembers)
			structMembers = EMPTY_LIST;
		return new AstNode(Token.STRUCT, structMembers);
	}

	@Override @DllInterface
	public AstNode dll(String libraryName, String userFunctionName,
			String returnType, AstNode dllParams) {
		if (null == dllParams)
			dllParams = EMPTY_LIST;
		return new AstNode(Token.DLL,
			new AstNode(Token.IDENTIFIER, libraryName),
			new AstNode(Token.STRING, userFunctionName),
			new AstNode(Token.IDENTIFIER, returnType),
			dllParams
		);
	}

	@Override @DllInterface
	public AstNode callback(AstNode callbackParams) {
		if (null == callbackParams)
			callbackParams = EMPTY_LIST;
		return new AstNode(Token.CALLBACK, callbackParams);
	}

	@Override @DllInterface
	public AstNode typeList(AstNode list, String name, boolean inTag,
			String baseType, Token storageType, String numElems) {
		AstNode type = new AstNode(storageType, new AstNode(Token.IDENTIFIER,
				baseType, new AstNode(Token.NUMBER, numElems), new AstNode(
						Token.IN, Boolean.toString(inTag))));
		return list(list, new AstNode(Token.IDENTIFIER, name, type));
	}

	private static AstNode list(AstNode list, AstNode next) {
		if (list == null)
			list = new AstNode(Token.LIST, new ArrayList<AstNode>());
		list.children.add(next);
		return list;
	}

	public static void main(String[] args) {
		String s = "function () { 10.Times() { } }";
		Lexer lexer = new Lexer(s);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		System.out.println(ast);
	}
}
