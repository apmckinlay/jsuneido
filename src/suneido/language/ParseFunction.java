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
		} else if (lexer.getKeyword() == null)
			return generator.expressionStatement(expression());

		switch (lexer.getKeyword()) {
		case FOREVER :
			match(FOREVER);
			return generator.foreverStatement(statement());
		case RETURN :
			// TODO expression
			return matchReturn(generator.returnStatement(null));
		}
		syntaxError(lexer.getKeyword() + " is not the valid start of a statment");
		return null;
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
