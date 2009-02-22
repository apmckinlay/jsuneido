package suneido.language;

import static suneido.language.Token.*;

/**
 * @author Andrew McKinlay
 */
public class ParseFunction<T> extends Parse<T> {

	ParseFunction(Lexer lexer, Generator<T> generator) {
		super(lexer, generator);
	}

	public T function() {
		match(FUNCTION);
		match(L_PAREN);
		// TODO parameters
		match(R_PAREN);
		return generator.function(compound());
	}

	public T compound() {
		T statements = null;
		match(L_CURLY);
		while (token != R_CURLY)
			statements = generator.statementList(statements, statement());
		match(R_CURLY);
		return statements;
	}

	public T statement() {
		if (token == L_CURLY)
			return compound();
		else if (token == SEMICOLON) {
			match();
			return null;
		}

		switch (lexer.getKeyword()) {
		case BREAK:
			return breakStatement();
		case CONTINUE:
			return continueStatement();
		case DO:
			return dowhileStatement();
		case FOR:
			return forStatement();
		case FOREVER:
			return foreverStatement();
		case IF:
			return ifStatement();
		case RETURN:
			return returnStatement();
		case SWITCH:
			return switchStatement();
		case THROW:
			return throwStatement();
		case TRY:
			return tryStatement();
		case WHILE:
			return whileStatement();
		default:
			return expressionStatement();
		}
	}

	private T breakStatement() {
		// TODO only allow in loop or block
		match(BREAK);
		matchIf(SEMICOLON);
		return generator.breakStatement();
	}

	private T continueStatement() {
		// TODO only allow in loop or block
		match(CONTINUE);
		matchIf(SEMICOLON);
		return generator.continueStatement();
	}

	private T dowhileStatement() {
		match(DO);
		T stat = statement();
		match(WHILE);
		T expr = optionalParensExpression();
		return generator.dowhileStatement(stat, expr);
	}

	private T expressionStatement() {
		return generator.expressionStatement(statementExpression());
	}

	private T statementExpression() {
		int prevStatementNest = statementNest;
		statementNest = 0;
		T expr = expression();
		statementNest = prevStatementNest;
		if (token == SEMICOLON || token == NEWLINE)
			match();
		else if (token != R_CURLY && lexer.getKeyword() != CATCH && lexer.getKeyword() != WHILE)
			syntaxError();
		return expr;
	}

	private T forStatement() {
		match(FOR);
		if (isForIn())
			return forInStatement();
		else
			return forClassicStatement();
	}

	private boolean isForIn() {
		Lexer ahead = new Lexer(lexer);
		return (token == L_PAREN ? ahead.next() : token) == IDENTIFIER &&
				ahead.next() == IDENTIFIER && ahead.getKeyword() == IN;
	}

	private T forInStatement() {
		int prevStatementNest = statementNest;
		boolean parens = matchIf(L_PAREN);
		String var = value;
		match(IDENTIFIER);
		match(IN);
		if (parens)
			statementNest = 0;
		T expr = expression();
		if (parens)
			match(R_PAREN);
		else
			matchIf(NEWLINE);
		statementNest = prevStatementNest;
		T stat = statement();
		return generator.forInStatement(var, expr, stat);
	}

	private T forClassicStatement() {
		match(L_PAREN);
		T expr1 = token == SEMICOLON ? null : expressionList();
		match(SEMICOLON);
		T expr2 = token == SEMICOLON ? null : expression();
		match(SEMICOLON);
		T expr3 = token == R_PAREN ? null : expressionList();
		match(R_PAREN);
		T stat = statement();
		return generator.forClassicStatement(expr1, expr2, expr3, stat);
	}

	private T expressionList() {
		T exprs = null;
		do
			exprs = generator.expressionList(exprs, expression());
		while (matchIf(COMMA));
		return exprs;
	}

	private T foreverStatement() {
		match(FOREVER);
		return generator.foreverStatement(statement());
	}

	private T ifStatement() {
		match(IF);
		T expr = optionalParensExpression();
		T truePart = statement();
		T falsePart = matchIf(ELSE) ? statement() : null;
		return generator.ifStatement(expr, truePart, falsePart);
	}

	private T optionalParensExpression() {
		int prevStatementNest = statementNest;
		boolean parens = matchIf(L_PAREN);
		if (parens)
			statementNest = 0;
		T expr = expression();
		if (parens)
			match(R_PAREN);
		else
			matchIf(NEWLINE);
		statementNest = prevStatementNest;
		return expr;
	}

	private T returnStatement() {
		matchKeepNewline(RETURN);
		T expr = endOfStatement() ? null : statementExpression();
		return generator.returnStatement(expr);
		}

	private T switchStatement() {
		match(SWITCH);
		T expr = optionalParensExpression();
		T cases = null;
		match(L_CURLY);
		while (matchIf(CASE)) {
			T values = null;
			do
				values = generator.caseValues(values, expression());
			while (matchIf(COMMA));
			cases = switchCase(cases, values);
		}
		if (matchIf(DEFAULT))
			cases = switchCase(cases, null);
		match(R_CURLY);
		return generator.switchStatement(expr, cases);
	}

	private T switchCase(T cases, T values) {
		match(COLON);
		T statements = null;
		while (token != R_CURLY &&
				lexer.getKeyword() != CASE &&
				lexer.getKeyword() != DEFAULT)
			statements = generator.statementList(statements, statement());
		return generator.switchCases(cases, values, statements);
	}

	private T throwStatement() {
		match(THROW);
		return generator.throwStatement(statementExpression());
	}

	private T tryStatement() {
		match(TRY);
		T tryStat = statement();
		T catcher = catcher();
		return generator.tryStatement(tryStat, catcher);
	}

	private T catcher() {
		if (!matchIf(CATCH))
			return null;
		String variable = null;
		String pattern = null;
		if (matchIf(L_PAREN)) {
			variable = value;
			match(IDENTIFIER);
			if (matchIf(COMMA)) {
				pattern = value;
				match(STRING);
			}
			match(R_PAREN);
		}
		return generator.catcher(variable, pattern, statement());
	}

	private T whileStatement() {
		match(WHILE);
		T expr = optionalParensExpression();
		T stat = statement();
		return generator.whileStatement(expr, stat);
	}

	private boolean endOfStatement() {
		return matchIf(NEWLINE) || matchIf(SEMICOLON) || token == R_CURLY;
	}

	private T expression() {
		// TODO move some of this to Parse method
		ParseExpression<T> p = new ParseExpression<T>(this);
		T result = p.expression();
		token = p.token;
		value = p.value;
		return result;
	}
}
