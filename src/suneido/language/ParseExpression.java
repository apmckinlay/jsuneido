package suneido.language;

import static suneido.language.Token.*;

public class ParseExpression<T> extends Parse<T> {

	ParseExpression(Lexer lexer, Generator<T> generator) {
		super(lexer, generator);
	}
	ParseExpression(Parse<T> parse) {
		super(parse);
	}

	T expression() {
		return conditionalExpression();
	}

	private T conditionalExpression() {
		T first = orExpression();
		if (token == Q_MARK) {
			++statementNest;
			match(Q_MARK);
			T t = expression();
			match(COLON);
			--statementNest;
			T f = expression();
			return generator.conditional(first, t, f);
		} else {
			return first;
		}
	}

	private T orExpression() {
		T result = andExpression();
		while (token == OR) {
			match();
			result = generator.binaryExpression(OR, result, andExpression());
		}
		return result;
	}

	private T andExpression() {
		T result = bitorExpression();
		while (token == AND) {
			match();
			result = generator.binaryExpression(AND, result, bitorExpression());
		}
		return result;
	}

	private T bitorExpression() {
		T result = bitxorExpression();
		while (token == BITOR) {
			match();
			result = generator.binaryExpression(BITOR, result, bitxorExpression());
		}
		return result;
	}

	private T bitxorExpression() {
		T result = bitandExpression();
		while (token == BITXOR) {
			match();
			result = generator.binaryExpression(BITXOR, result, bitandExpression());
		}
		return result;
	}

	private T bitandExpression() {
		T result = isExpression();
		while (token == BITAND) {
			match();
			result = generator.binaryExpression(BITAND, result, isExpression());
		}
		return result;
	}

	private T isExpression() {
		T result = compareExpression();
		while (token == IS || token == ISNT || token == MATCH || token == MATCHNOT) {
			Token op = token;
			match();
			result = generator.binaryExpression(op, result, compareExpression());
		}
		return result;
	}

	private T compareExpression() {
		T result = shiftExpression();
		while (token == LT || token == LTE || token == GT || token == GTE) {
			Token op = token;
			match();
			result = generator.binaryExpression(op, result, shiftExpression());
		}
		return result;
	}

	private T shiftExpression() {
		T result = addExpression();
		while (token == LSHIFT || token == RSHIFT) {
			Token op = token;
			match();
			result = generator.binaryExpression(op, result, addExpression());
		}
		return result;
	}

	private T addExpression() {
		T result = mulExpression();
		while (token == ADD || token == SUB || token == CAT) {
			Token op = token;
			match();
			result = generator.binaryExpression(op, result, mulExpression());
		}
		return result;
	}

	private T mulExpression() {
		T result = unaryExpression();
		while (token == MUL || token == DIV || token == MOD) {
			Token op = token;
			match();
			result = generator.binaryExpression(op, result, unaryExpression());
		}
		return result;
	}

	private T unaryExpression() {
		switch (token) {
		case ADD:
		case SUB:
		case NOT:
		case BITNOT:
			Token op = token;
			match();
			return generator.unaryExpression(op, unaryExpression());
		default:
			if (lexer.getKeyword() == NEW) {
				return newExpression();
			} else
				return term();
		}
	}
	private T newExpression() {
		match(NEW);
		T term = term(true);
		T args = token == L_PAREN ? arguments() : null;
		return generator.newExpression(term, args);
	}

	private T term() {
		return term(false);
	}

	private T term(boolean newTerm) {
		T term = null;
		boolean lvalue = true;
		Token incdec = null;

		if (token == INC || token == DEC) {
			incdec = token;
			match();
		}
		switch (token) {
		case NUMBER:
		case STRING:
		case HASH:
			term = constant();
			lvalue = false;
			break;
		case L_CURLY:
			term = block();
			lvalue = false;
			break;
		case IDENTIFIER:
			switch (lexer.getKeyword()) {
			case FUNCTION:
			case CLASS:
				term = constant();
				lvalue = false;
				break;
			default:
				term = generator.identifier(lexer.getValue());
				match(IDENTIFIER);
			}
			break;
		case DOT:
			term = generator.self();
			break;
		case L_PAREN:
			match(L_PAREN);
			term = expression();
			match(R_PAREN);
			lvalue = false;
			break;
		default:
			syntaxError();
		}

		while (true) {
			if (newTerm && token == L_PAREN)
				return term;
			if (matchIf(DOT)) {
				term = generator.member(term, lexer.getValue());
				match(IDENTIFIER);
				lvalue = true;
			} else if (matchIf(L_BRACKET)) {
				term = generator.subscript(term, expression());
				match(R_BRACKET);
				lvalue = true;
			} else if (token == L_PAREN || token == L_CURLY) {
				term = generator.functionCall(term, arguments());
				lvalue = false;
			} else
				break;
		}

		if (incdec != null) {
			if (!lvalue)
				syntaxError("lvalue required");
			term = generator.preIncDec(incdec, term);
		} else if (isEq(token)) {
			if (!lvalue)
				syntaxError("lvalue required");
			Token op = token;
			match();
			term = generator.assignment(term, op, expression());
		} else if (token == INC || token == DEC) {
			if (!lvalue)
				syntaxError("lvalue required");
			term = generator.postIncDec(token, term);
			match();
		}
		return term;
	}

	private T arguments() {
		T args = null;
		if (matchIf(L_PAREN)) {
			if (matchIf(AT))
				return atArgument();
			else
				args = argumentList();
		}
		if (token == L_CURLY)
			args = generator.expressionList(args, block());
		return args;
	}
	private T atArgument() {
		String n = null;
		if (matchIf(ADD)) {
			n = lexer.getValue();
			match(NUMBER);
		}
		T expr = expression();
		match(R_PAREN);
		return generator.atArgument(n, expr);
	}

	private T argumentList() {
		T args = null;
		String keyword = null;
		while (token != R_PAREN) {
			if (lookAhead() == COLON) {
				keyword = keyword();
			} else if (keyword != null)
				syntaxError("un-named arguments must come before named arguments");
			args = generator.argumentList(args, keyword, expression());
			matchIf(COMMA);
		}
		match(R_PAREN);
		return args;
	}
	private String keyword() {
		if (token != IDENTIFIER && token != STRING && token != NUMBER)
			syntaxError("invalid keyword");
		String keyword = lexer.getValue();
		match();
		match(COLON);
		return keyword;
	}

	private T block() {
		match(L_CURLY);
		T params = token == BITOR ? blockParams() : null;
		T statements = statementList();
		match(R_CURLY);
		return generator.block(params, statements);
	}

	private T blockParams() {
		match(BITOR);
		T params = null;
		if (matchIf(AT)) {
			params = generator.parameters(params, "@" + lexer.getValue(), null);
			match(IDENTIFIER);
		} else
			while (token == IDENTIFIER) {
				params = generator.parameters(params, lexer.getValue(), null);
				match(IDENTIFIER);
				matchIf(COMMA);
			}
		match(BITOR);
		return params;
	}
	private boolean isEq(Token token) {
		switch (token) {
		case EQ:
		case ADDEQ:
		case SUBEQ:
		case CATEQ:
		case MULEQ:
		case DIVEQ:
		case MODEQ:
		case LSHIFTEQ:
		case RSHIFTEQ:
		case BITOREQ:
		case BITANDEQ:
		case BITXOREQ:
			return true;
		default:
			return false;
		}
	}

	private T constant() {
		ParseConstant<T> p = new ParseConstant<T>(this);
		T result = p.constant();
		token = p.token;
		return result;
	}

	private T statementList() {
		ParseFunction<T> p = new ParseFunction<T>(this);
		T result = p.statementList();
		token = p.token;
		return result;
	}
}
