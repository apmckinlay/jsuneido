package suneido.language;

import java.util.ArrayList;


public class AstGenerator extends Generator<AstNode> {

	@Override
	public AstNode functionEnd(AstNode params, AstNode compound) {
		return new AstNode(Token.FUNCTION, params, compound);
	}

	@Override
	public AstNode parameters(AstNode list, String name, AstNode defaultValue) {
		return list(list, new AstNode(Token.IDENTIFIER, name, defaultValue));
	}

	@Override
	public AstNode memberDefinition(AstNode name, AstNode value) {
		return new AstNode(Token.MEMBER, name, value);
	}

	@Override
	public AstNode memberList(MType which, AstNode list, AstNode member) {
		return list(list, member);
	}

	@Override
	public AstNode objectEnd(MType which, AstNode members) {
		return new AstNode(which == MType.CLASS ? Token.CLASS :
			which == MType.RECORD ? Token.RECORD : Token.OBJECT,
			members == null ? AstNode.emptyList : members.children);
	}

	// statements

	@Override
	public AstNode statementList(AstNode list, AstNode next) {
		return list(list, next);
	}

	@Override
	public AstNode dowhileStatement(AstNode statement, AstNode expr, Object label) {
		return new AstNode(Token.DO, statement, expr);
	}

	@Override
	public AstNode whileStatement(AstNode expr, AstNode statement, Object loop) {
		return new AstNode(Token.DO, statement, expr);
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
		return new AstNode(Token.FOR_IN, var, statement, expr);
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
	public Object conditionalTrue(Object label, AstNode first) {
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
	public AstNode switchStatement(AstNode expr, AstNode cases, Object labels) {
		return new AstNode(Token.SWITCH, expr, cases);
	}

	@Override
	public AstNode switchCases(AstNode cases, AstNode values,
			AstNode statements, Object labels, boolean moreCases) {
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
		return new AstNode(op, expr1, expr2);
	}

	@Override
	public AstNode and(Object label, AstNode expr1, AstNode expr2) {
		return new AstNode(Token.AND, expr1, expr2);
	}

	@Override
	public AstNode or(Object label, AstNode expr1, AstNode expr2) {
		return new AstNode(Token.OR, expr1, expr2);
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
	public AstNode functionCall(AstNode function, AstNode arguments) {
		return new AstNode(Token.CALL, function, arguments);
	}

	@Override
	public AstNode argumentList(AstNode list, Object keyword, AstNode expr) {
		return list(list, new AstNode(Token.ARG, (String) keyword, expr));
	}

	@Override
	public AstNode assignment(AstNode term, Token op, AstNode expr) {
		return new AstNode(op, term, expr);
	}

	@Override
	public AstNode expressionList(AstNode list, AstNode expr) {
		return list(list, expr);
	}

	private AstNode list(AstNode list, AstNode next) {
		if (list == null)
			list = new AstNode(Token.LIST, new ArrayList<AstNode>());
		list.children.add(next);
		return list;
	}

	public static void main(String[] args) {
		String s = "function () { return }";
		Lexer lexer = new Lexer(s);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		System.out.println(ast);
	}

}
