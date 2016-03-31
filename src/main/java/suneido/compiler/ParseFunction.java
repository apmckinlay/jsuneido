/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.Token.*;

public class ParseFunction<T, G extends Generator<T>> extends Parse<T, G> {

	public ParseFunction(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	ParseFunction(Parse<T, G> parse) {
		super(parse);
	}

	public T parse() {
		return matchReturn(EOF, function());
	}

	public T function() {
		matchSkipNewlines(FUNCTION);
		return functionWithoutKeyword(false);
	}
	protected T functionWithoutKeyword(boolean inClass) {
		int lineNumber = lexer.getLineNumber();
		// NOTE: lineNumber is starting at arg list LPAREN, which is not ideal.
		//       Better would be starting on the "function" keyword or member
		//       name, as the case may be.
		T params = parameters();
		T body = compound(NORMAL);
		return generator.function(params, body, inClass, lineNumber);
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
				boolean dot = matchIf(DOT);
				String name = lexer.getValue();
				if (dot)
					name = "." + name;
				match(IDENTIFIER);
				if (matchIf(EQ)) {
					if (idString())
						syntaxError("parameter defaults must be constants");
					defaultValue = constant();
				} else if (defaultValue != null)
					syntaxError("default parameters must come last");
				params = generator.parameters(params, name, defaultValue);
				matchIf(COMMA);
			}
		}
		matchSkipNewlines(R_PAREN);
		return params;
	}

	private boolean idString() {
		if (token != IDENTIFIER)
			return false;
		switch (lexer.getKeyword()) {
		case FUNCTION:
		case CLASS:
		case STRUCT:
		case DLL:
		case CALLBACK:
		case TRUE:
		case FALSE:
			return false;
		default:
			return lookAhead() != L_CURLY;
		}
	}

	public T compound(Context context) {
		match(L_CURLY);
		T statements = statementList(context);
		match(R_CURLY);
		return statements;
	}

	public T statementList(Context context) {
		T statements = null;
		while (token != R_CURLY) {
			statements = generator.statementList(statements, statement(context));
		}
		return statements;
	}

	public T statement(Context context) {
		while (token == NEWLINE || token == WHITE || token == COMMENT)
			match();
		if (token == L_CURLY)
			return compound(context);
		else if (matchIf(SEMICOLON))
			return null;

		switch (lexer.getKeyword()) {
		case BREAK:
			return breakStatement(context);
		case CONTINUE:
			return continueStatement(context);
		case DO:
			return dowhileStatement();
		case FOR:
			return forStatement();
		case FOREVER:
			return foreverStatement();
		case IF:
			return ifStatement(context);
		case RETURN:
			return returnStatement(context);
		case SWITCH:
			return switchStatement(context);
		case THROW:
			return throwStatement();
		case TRY:
			return tryStatement(context);
		case WHILE:
			return whileStatement();
		default:
			return expressionStatement();
		}
	}

	private T breakStatement(Context context) {
		int lineNumber = lexer.getLineNumber();
		match(BREAK);
		matchIf(SEMICOLON);
		if (! context.breakAllowed)
			syntaxError("break can only be used in a loop, switch case, or block");
		return generator.breakStatement(lineNumber);
	}

	private T continueStatement(Context context) {
		int lineNumber = lexer.getLineNumber();
		match(CONTINUE);
		matchIf(SEMICOLON);
		if (! context.continueAllowed)
			syntaxError("continue can only be used in a loop or block");
		return generator.continueStatement(lineNumber);
	}

	private T dowhileStatement() {
		match(DO);
		T statement = statement(LOOP);
		match(WHILE);
		T expr = optionalParensExpression();
		return generator.dowhileStatement(statement, expr);
	}

	private T expressionStatement() {
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
		statementNest = prevStatementNest;
		expectingCompound = false;
		T stat = statement(LOOP);
		return generator.forInStatement(var, expr, stat);
	}

	private T forClassicStatement() {
		match(L_PAREN);
		T init = (token == SEMICOLON) ? null : expressionList();
		match(SEMICOLON);

		T cond = (token == SEMICOLON) ? null : expression();
		match(SEMICOLON);

		T incr = token == R_PAREN ? null : expressionList();
		match(R_PAREN);

		T stat = statement(LOOP);

		return generator.forClassicStatement(init, cond, incr, stat);
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
		T statement = statement(LOOP);
		return generator.foreverStatement(statement);
	}

	private T ifStatement(Context context) {
		match(IF);
		T expr = optionalParensExpression();
		T truePart = statement(context);
		T falsePart = null;
		if (matchIf(ELSE))
			falsePart = statement(context);
		return generator.ifStatement(expr, truePart, falsePart);
	}

	private T optionalParensExpression() {
		int prevStatementNest = statementNest;
		boolean parens = matchIf(L_PAREN);
		if (! parens) {
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
		int lineNumber = lexer.getLineNumber();
		matchKeepNewline(RETURN);
		T expr = endOfStatement() ? null : expressionStatement();
		return generator.returnStatement(expr, context, lineNumber);
		}

	private T switchStatement(Context context) {
		context = new Context(true, context.continueAllowed);
		int lineNumber = lexer.getLineNumber();
		match(SWITCH);
		T expr = (token == L_CURLY)
			? generator.constant(generator.boolTrue(lineNumber))
			: generator.rvalue(optionalParensExpression());
		T cases = null;
		match(L_CURLY);
		while (matchIf(CASE))
			cases = switchCase(cases, context);
		if (matchIf(DEFAULT))
			cases = switchCaseBody(cases, null, context);
		else {
			T statements = generator.statementList(null, generator
					.throwStatement(generator.constant(generator.string(
							"unhandled switch case", lineNumber)), lineNumber));
			cases = generator.switchCases(cases, null, statements);
		}
		match(R_CURLY);
		return generator.switchStatement(expr, cases);
	}
	private T switchCase(T cases, Context context) {
		T values = null;
		do {
			T value = expression();
			values = generator.caseValues(values, value);
		} while (matchIf(COMMA));
		cases = switchCaseBody(cases, values, context);
		return cases;
	}
	private T switchCaseBody(T cases, T values, Context context) {
		match(COLON);
		T statements = null;
		while (token != R_CURLY &&
				lexer.getKeyword() != CASE &&
				lexer.getKeyword() != DEFAULT) {
			statements = generator.statementList(statements, statement(context));
		}
		return generator.switchCases(cases, values, statements);
	}

	private T throwStatement() {
		int lineNumber = lexer.getLineNumber();
		match(THROW);
		return generator.throwStatement(expressionStatement(), lineNumber);
	}

	private T tryStatement(Context context) {
		match(TRY);
		T tryStat = statement(context);
		T catcher = catcher(context);
		return generator.tryStatement(tryStat, catcher);
	}

	private T catcher(Context context) {
		if (!matchIf(CATCH)) {
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
		T catchStat = statement(context);
		return generator.catcher(variable, pattern, catchStat);
	}

	private T whileStatement() {
		match(WHILE);
		T expr = optionalParensExpression();
		T stat = statement(LOOP);
		return generator.whileStatement(expr, stat);
	}

	private boolean endOfStatement() {
		return matchIf(NEWLINE) || matchIf(SEMICOLON) || token == R_CURLY;
	}

	private T expression() {
		ParseExpression<T, G> p = new ParseExpression<>(this);
		T result = p.expression();
		token = p.token;
		return result;
	}

	private T constant() {
		ParseConstant<T, G> p = new ParseConstant<>(this);
		T result = p.constant();
		token = p.token;
		return result;
	}

	static class Context {
		final boolean breakAllowed;
		final boolean continueAllowed;
		Context(boolean breakAllowed, boolean continueAllowed) {
			this.breakAllowed = breakAllowed;
			this.continueAllowed = continueAllowed;
		}
	}
	static final Context LOOP = new Context(true, true);
	static final Context NORMAL = new Context(false, false);

//	public static void main(String[] args) {
//		AstNode ast = Compiler.parse("function (.a, _b, ._c) { a + b }");
//		System.out.println(ast);
//	}

}
