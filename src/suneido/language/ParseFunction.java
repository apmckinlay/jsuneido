package suneido.language;

import static suneido.language.Token.*;
import suneido.language.Generator.FuncOrBlock;

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
		generator.startFunction(FuncOrBlock.FUNC);
		T params = parameters();
		T body = compound(null);
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
	public T compound(Object loop) {
		match(L_CURLY);
		T statements = statementList(loop);
		match(R_CURLY);
		return statements;
	}

	public T statementList() {
		return statementList(null);
	}
	public T statementList(Object loop) {
		T statements = null;
		while (token != R_CURLY) {
			generator.afterStatement(statements);
			statements = generator.statementList(statements, statement(loop));
		}
		return statements;
	}

	public T statement(Object loop) {
		if (token == L_CURLY)
			return compound(loop);
		else if (matchIf(SEMICOLON))
			return null;

		switch (lexer.getKeyword()) {
		case BREAK:
			return breakStatement(loop);
		case CONTINUE:
			return continueStatement(loop);
		case DO:
			return dowhileStatement();
		case FOR:
			return forStatement();
		case FOREVER:
			return foreverStatement();
		case IF:
			return ifStatement(loop);
		case RETURN:
			return returnStatement(loop);
		case SWITCH:
			return switchStatement(loop);
		case THROW:
			return throwStatement();
		case TRY:
			return tryStatement(loop);
		case WHILE:
			return whileStatement();
		default:
			return expressionStatement();
		}
	}

	private T breakStatement(Object loop) {
		match(BREAK);
		matchIf(SEMICOLON);
		if (loop == null)
			syntaxError("break can only be used within a loop");
		return generator.breakStatement(loop);
	}

	private T continueStatement(Object loop) {
		match(CONTINUE);
		matchIf(SEMICOLON);
		if (loop == null)
			syntaxError("continue can only be used within a loop");
		return generator.continueStatement(loop);
	}

	private T dowhileStatement() {
		match(DO);
		Object loop = generator.loop();
		T statement = statement(loop);
		generator.afterStatement(statement);
		match(WHILE);
		T expr = optionalParensExpression();
		return generator.dowhileStatement(statement, expr, loop);
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
		else if (token != R_CURLY && lexer.getKeyword() != CATCH
				&& lexer.getKeyword() != WHILE && lexer.getKeyword() != ELSE)
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
		if (!parens) {
			statementNest = 0;
			expectingCompound = true;
		}
		T expr = expression();
		if (parens)
			match(R_PAREN);
		else
			matchIf(NEWLINE);
		Object loop = generator.forInExpression(var, expr);
		statementNest = prevStatementNest;
		expectingCompound = false;
		T stat = statement(loop);
		return generator.forInStatement(var, expr, stat, loop);
	}

	private T forClassicStatement() {
		T init = forInit();

		String condSource = saveCond();

		boolean hasIncr = (token != R_PAREN);
		Object label = hasIncr ? generator.forStart() : null;
		Object loop = generator.loop();

		T incr = forIncr(label);

		T cond = forCond(condSource, loop);

		T stat = statement(loop);
		return generator.forClassicStatement(init, cond, incr, stat, loop);
	}
	private T forInit() {
		match(L_PAREN);
		T init = token == SEMICOLON ? null : expressionList();
		match(SEMICOLON);
		return init;
	}
	private String saveCond() {
		String condSource = null;
		if (token != SEMICOLON) {
			int pos = lexer.position();
			while (token != SEMICOLON)
				match();
			condSource = lexer.from(pos);
		}
		match(SEMICOLON);
		return condSource;
	}
	private T forCond(String condSource, Object loop) {
		if (condSource == null)
			return null;
		Lexer lex = new Lexer(condSource);
		ParseExpression<T, Generator<T>> pc =
				new ParseExpression<T, Generator<T>>(lex, generator);
		T cond = pc.parse();
		generator.forCondition(cond, loop);
		return cond;
	}
	private T forIncr(Object label) {
		T incr = token == R_PAREN ? null : expressionList();
		match(R_PAREN);
		generator.forIncrement(label);
		return incr;
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
		Object loop = generator.loop();
		T statement = statement(loop);
		return generator.foreverStatement(statement, loop);
	}

	private T ifStatement(Object loop) {
		match(IF);
		T expr = optionalParensExpression();
		Object label = generator.ifExpr(expr);
		T truePart = statement(loop);
		generator.ifThen(label, truePart);
		T falsePart = null;
		if (matchIf(ELSE)) {
			label = generator.ifElse(label);
			falsePart = statement(loop);
		}
		return generator.ifStatement(expr, truePart, falsePart, label);
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

	private T returnStatement(Object context) {
		matchKeepNewline(RETURN);
		T expr = endOfStatement() ? null : statementExpression();
		return generator.returnStatement(expr, context);
		}

	private T switchStatement(Object loop) {
		match(SWITCH);
		Object labels = generator.startSwitch();
		T expr = optionalParensExpression();
		T cases = null;
		match(L_CURLY);
		while (matchIf(CASE))
			cases = switchCase(cases, labels, loop);
		if (matchIf(DEFAULT)) {
			generator.startCase(labels);
			cases = switchCaseBody(cases, null, labels, loop);
		}
		match(R_CURLY);
		return generator.switchStatement(expr, cases, labels);
	}
	private T switchCase(T cases, Object labels, Object loop) {
		generator.startCase(labels);
		T values = null;
		do {
			generator.startCaseValue();
			T value = expression();
			values = generator.caseValues(values, value, labels, token == COMMA);
		} while (matchIf(COMMA));
		cases = switchCaseBody(cases, values, labels, loop);
		return cases;
	}
	private T switchCaseBody(T cases, T values, Object labels, Object loop) {
		match(COLON);
		generator.startCaseBody(labels);
		T statements = null;
		while (token != R_CURLY &&
				lexer.getKeyword() != CASE &&
				lexer.getKeyword() != DEFAULT)
			statements = generator.statementList(statements, statement(loop));
		return generator.switchCases(cases, values, statements, labels);
	}

	private T throwStatement() {
		match(THROW);
		return generator.throwStatement(statementExpression());
	}

	private T tryStatement(Object loop) {
		match(TRY);
		Object trycatch = generator.startTry();
		T tryStat = statement(loop);
		generator.afterStatement(tryStat);
		T catcher = catcher(loop, trycatch);
		return generator.tryStatement(tryStat, catcher, trycatch);
	}

	private T catcher(Object loop, Object trycatch) {
		if (!matchIf(CATCH)) {
			generator.startCatch(null, null, trycatch);
			return null;
		}
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
		generator.startCatch(variable, pattern, trycatch);
		T catchStat = statement(loop);
		generator.afterStatement(catchStat);
		return generator.catcher(variable, pattern, catchStat);
	}

	private T whileStatement() {
		match(WHILE);
		Object loop = generator.loop();
		T expr = optionalParensExpression();
		generator.whileExpr(expr, loop);
		T stat = statement(loop);
		return generator.whileStatement(expr, stat, loop);
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
