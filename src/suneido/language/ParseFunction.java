package suneido.language;

import static suneido.language.Token.*;

/**
 * @author Andrew McKinlay
 */
public class ParseFunction<T, G extends Generator<T>> extends Parse<T, G> {

	ParseFunction(Lexer lexer, G generator) {
		super(lexer, generator);
		expectingCompound = false;
	}
	ParseFunction(Parse<T, G> parse) {
		super(parse);
		expectingCompound = false;
	}

	public T parse() {
		return matchReturn(EOF, function());
	}

	public T function() {
		matchSkipNewlines(FUNCTION);
		return functionWithoutKeyword();
	}
	protected T functionWithoutKeyword() {
		generator.startFunction();
		T params = parameters();
		T body = compound();
		return generator.function(params, body);
	}

	private T parameters() {
		match(L_PAREN);
		T params = null;
		if (matchIf(AT)) {
			params = generator.parameters(params, "@" + lexer.getValue(), null);
			match(IDENTIFIER);
		} else {
			T defaultValue = null;
			while (token != R_PAREN) {
				String name = lexer.getValue();
				match(IDENTIFIER);
				if (matchIf(EQ))
					defaultValue = constant();
				else if (defaultValue != null)
					syntaxError("default parameters must come last");
				params = generator.parameters(params, name, defaultValue);
				matchIf(COMMA);
			}
		}
		matchSkipNewlines(R_PAREN);
		return params;
	}
	public T compound() {
		match(L_CURLY);
		T statements = statementList();
		match(R_CURLY);
		return statements;
	}

	public T statementList() {
		T statements = null;
		while (token != R_CURLY) {
			generator.beforeStatement(statements);
			statements = generator.statementList(statements, statement());
		}
		return statements;
	}

	public T statement() {
		if (token == L_CURLY)
			return compound();
		else if (matchIf(SEMICOLON))
			return null;

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
		match(BREAK);
		matchIf(SEMICOLON);
		return generator.breakStatement();
	}

	private T continueStatement() {
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
		String var = lexer.getValue();
		match(IDENTIFIER);
		match(IN);
		if (!parens)
			{
			statementNest = 0;
			expectingCompound = true;
		}
		T expr = expression();
		if (parens)
			match(R_PAREN);
		else
			matchIf(NEWLINE);
		statementNest = prevStatementNest;
		expectingCompound = false;
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
		if (!parens)
			{
			statementNest = 0;
			expectingCompound = true;
		}
		T expr = expression();
		if (parens)
			match(R_PAREN);
		else
			matchIf(NEWLINE);
		statementNest = prevStatementNest;
		expectingCompound = false;
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
			if (token == IDENTIFIER) {
				variable = lexer.getValue();
				match(IDENTIFIER);
				if (matchIf(COMMA)) {
					pattern = lexer.getValue();
					match(STRING);
				}
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
		ParseExpression<T, G> p = new ParseExpression<T, G>(this);
		T result = p.expression();
		token = p.token;
		return result;
	}

	private T constant() {
		ParseConstant<T, G> p = new ParseConstant<T, G>(this);
		T result = p.constant();
		token = p.token;
		return result;
	}

}
