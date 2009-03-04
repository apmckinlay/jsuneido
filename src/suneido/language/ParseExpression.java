package suneido.language;

import static suneido.language.Token.*;

public class ParseExpression<T, G extends Generator<T>> extends Parse<T, G> {

	ParseExpression(Lexer lexer, G generator) {
		super(lexer, generator);
	}
	public ParseExpression(Parse<T, G> parse) {
		super(parse);
	}

	public T expression() {
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
			matchSkipNewlines();
			result = generator.binaryExpression(OR, result, andExpression());
		}
		return result;
	}

	private T andExpression() {
		T result = bitorExpression();
		while (token == AND) {
			matchSkipNewlines();
			result = generator.binaryExpression(AND, result, bitorExpression());
		}
		return result;
	}

	private T bitorExpression() {
		T result = bitxorExpression();
		while (token == BITOR) {
			matchSkipNewlines();
			result = generator.binaryExpression(BITOR, result, bitxorExpression());
		}
		return result;
	}

	private T bitxorExpression() {
		T result = bitandExpression();
		while (token == BITXOR) {
			matchSkipNewlines();
			result = generator.binaryExpression(BITXOR, result, bitandExpression());
		}
		return result;
	}

	private T bitandExpression() {
		T result = isExpression();
		while (token == BITAND) {
			matchSkipNewlines();
			result = generator.binaryExpression(BITAND, result, isExpression());
		}
		return result;
	}

	private T isExpression() {
		T result = compareExpression();
		while (token == IS || token == ISNT || token == MATCH || token == MATCHNOT) {
			Token op = token;
			matchSkipNewlines();
			result = generator.binaryExpression(op, result, compareExpression());
		}
		return result;
	}

	private T compareExpression() {
		T result = shiftExpression();
		while (token == LT || token == LTE || token == GT || token == GTE) {
			Token op = token;
			matchSkipNewlines();
			result = generator.binaryExpression(op, result, shiftExpression());
		}
		return result;
	}

	private T shiftExpression() {
		T result = addExpression();
		while (token == LSHIFT || token == RSHIFT) {
			Token op = token;
			matchSkipNewlines();
			result = generator.binaryExpression(op, result, addExpression());
		}
		return result;
	}

	private T addExpression() {
		T result = mulExpression();
		while (token == ADD || token == SUB || token == CAT) {
			Token op = token;
			matchSkipNewlines();
			result = generator.binaryExpression(op, result, mulExpression());
		}
		return result;
	}

	private T mulExpression() {
		T result = unaryExpression();
		while (token == MUL || token == DIV || token == MOD) {
			Token op = token;
			matchSkipNewlines();
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
		case DOT:
			term = generator.self();
			break;
		case L_PAREN:
			match(L_PAREN);
			term = expression();
			match(R_PAREN);
			lvalue = false;
			break;
		case L_BRACKET:
			match(L_BRACKET);
			term = generator.functionCall(generator.identifier("Record"),
					argumentList(R_BRACKET));
			lvalue = false;
			break;
		case IDENTIFIER:
			switch (lexer.getKeyword()) {
			case FUNCTION:
			case CLASS:
			case DLL:
			case STRUCT:
			case CALLBACK:
				term = constant();
				lvalue = false;
				break;
			default:
				if (isGlobal(lexer.getValue()) &&
						lookAhead(! expectingCompound) == L_CURLY) {
					term = constant();
					lvalue = false;
				} else {
					term = generator.identifier(lexer.getValue());
					match(IDENTIFIER);
				}
			}
			break;
		default:
			syntaxError();
		}

		while (true) {
			if (newTerm && token == L_PAREN)
				return term;
			if (token == DOT) {
				matchSkipNewlines(DOT);
				term = generator.member(term, lexer.getValue());
				match(IDENTIFIER);
				if (!expectingCompound && token == NEWLINE && lookAhead() == L_CURLY)
					match();
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
		} else if (token.assign()) {
			if (!lvalue)
				syntaxError("lvalue required");
			Token op = token;
			matchSkipNewlines();
			term = generator.assignment(term, op, expression());
		} else if (token == INC || token == DEC) {
			if (!lvalue)
				syntaxError("lvalue required");
			term = generator.postIncDec(token, term);
			match();
		}
		return term;
	}

	private boolean isGlobal(String value) {
		char c = value.charAt(0);
		if (c == '_')
			c = value.charAt(1);
		return Character.isUpperCase(c);
	}
	private T arguments() {
		T args = null;
		if (matchIf(L_PAREN)) {
			if (matchIf(AT))
				return atArgument();
			else
				args = argumentList(R_PAREN);
		}
		if (token == NEWLINE && !expectingCompound && lookAhead() == L_CURLY)
			match();
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

	private T argumentList(Token closing) {
		T args = null;
		String keyword = null;
		while (token != closing) {
			if (lookAhead() == COLON) {
				keyword = keyword();
			} else if (keyword != null)
				syntaxError("un-named arguments must come before named arguments");
			T value;
			if (keyword != null &&
					(token == COMMA || token == closing || lookAhead() == COLON))
				value = generator.bool("true");
			else
				value = expression();
			args = generator.argumentList(args, keyword, value);
			matchIf(COMMA);
		}
		match(closing);
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

	private T constant() {
		ParseConstant<T, G> p = new ParseConstant<T, G>(this);
		T result = p.constant();
		token = p.token;
		return result;
	}

	private T statementList() {
		ParseFunction<T, G> p = new ParseFunction<T, G>(this);
		T result = p.statementList();
		token = p.token;
		return result;
	}
}
