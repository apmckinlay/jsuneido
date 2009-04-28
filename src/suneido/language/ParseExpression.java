package suneido.language;

import static suneido.language.Token.*;
import suneido.language.Generator.FuncOrBlock;
import suneido.language.ParseExpression.Value.ThisOrSuper;

public class ParseExpression<T, G extends Generator<T>> extends Parse<T, G> {
	boolean EQ_as_IS = false;

	public ParseExpression(Lexer lexer, G generator) {
		super(lexer, generator);
	}
	public ParseExpression(Parse<T, G> parse) {
		super(parse);
	}

	public void eq_as_is() {
		EQ_as_IS = true;
	}

	public T parse() {
		return matchReturn(EOF, expression());
	}

	public T expression() {
		return conditionalExpression();
	}

	private T conditionalExpression() {
		T first = orExpression();
		if (token == Q_MARK) {
			Object label = generator.ifExpr(first);
			++statementNest;
			match(Q_MARK);
			T t = expression();
			label = generator.conditionalTrue(label, t);
			match(COLON);
			--statementNest;
			T f = expression();
			return generator.conditional(first, t, f, label);
		} else {
			return first;
		}
	}

	private T orExpression() {
		Object label = null;
		T result = andExpression();
		while (token == OR) {
			matchSkipNewlines();
			label = generator.or(label);
			generator.rvalue(result);
			result = generator.or(result, andExpression());
		}
		generator.orEnd(label);
		return result;
	}

	private T andExpression() {
		Object label = null;
		T result = inExpression();
		while (token == AND) {
			matchSkipNewlines();
			label = generator.and(label);
			generator.rvalue(result);
			result = generator.and(result, inExpression());
		}
		generator.andEnd(label);
		return result;
	}

	private T inExpression() {
		T expr = bitorExpression();
		if (matchIf(IN)) {
			expr = generator.in(expr, null);
			match(L_PAREN);
			while (token != R_PAREN) {
				expr = generator.in(expr, constant());
				matchIf(COMMA);
			}
			match(R_PAREN);
		}
		return expr;
	}

	private T bitorExpression() {
		T result = bitxorExpression();
		while (token == BITOR) {
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(BITOR, result, bitxorExpression());
		}
		return result;
	}

	private T bitxorExpression() {
		T result = bitandExpression();
		while (token == BITXOR) {
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(BITXOR, result, bitandExpression());
		}
		return result;
	}

	private T bitandExpression() {
		T result = isExpression();
		while (token == BITAND) {
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(BITAND, result, isExpression());
		}
		return result;
	}

	private T isExpression() {
		T result = compareExpression();
		while (token == IS || token == ISNT ||
				token == MATCH || token == MATCHNOT ||
				(EQ_as_IS && token == EQ)) {
			Token op = (token == EQ ? IS : token);
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(op, result, compareExpression());
		}
		return result;
	}

	private T compareExpression() {
		T result = shiftExpression();
		while (token == LT || token == LTE || token == GT || token == GTE) {
			Token op = token;
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(op, result, shiftExpression());
		}
		return result;
	}

	private T shiftExpression() {
		T result = addExpression();
		while (token == LSHIFT || token == RSHIFT) {
			Token op = token;
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(op, result, addExpression());
		}
		return result;
	}

	private T addExpression() {
		T result = mulExpression();
		while (token == ADD || token == SUB || token == CAT) {
			Token op = token;
			matchSkipNewlines();
			generator.rvalue(result);
			result = generator.binaryExpression(op, result, mulExpression());
		}
		return result;
	}

	private T mulExpression() {
		T result = unaryExpression();
		while (token == MUL || token == DIV || token == MOD) {
			Token op = token;
			matchSkipNewlines();
			generator.rvalue(result);
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
			return lexer.getKeyword() == NEW ? newExpression() : term();
		}
	}
	private T newExpression() {
		match(NEW);
		T term = term(true);
		generator.newCall();
		T args = token == L_PAREN ? arguments() : null;
		return generator.newExpression(term, args);
	}

	public static class Value<T> {
		public enum Type { IDENTIFIER, MEMBER, SUBSCRIPT };
		public enum ThisOrSuper { THIS, SUPER };
		Type type = null;
		String id;
		T expr;
		ThisOrSuper thisOrSuper;

		void identifier(String id) {
			type = Type.IDENTIFIER;
			this.id = id;
			expr = null;
			thisOrSuper = null;
		}
		void member(String id, ThisOrSuper thisOrSuper) {
			type = Type.MEMBER;
			this.id = id;
			expr = null;
			this.thisOrSuper = thisOrSuper;
		}
		void subscript(T expr, ThisOrSuper thisOrSuper) {
			type = Type.SUBSCRIPT;
			id = null;
			this.expr = expr;
			this.thisOrSuper = thisOrSuper;
		}
		void clear() {
			type = null;
			id = null;
			expr = null;
			thisOrSuper = null;
		}
		boolean isSet() {
			return type != null;
		}
		boolean isIdentifier() {
			return type == Type.IDENTIFIER;
		}
		boolean isMember() {
			return type == Type.MEMBER;
		}
	}

	private T term() {
		return term(false);
	}

	private T term(boolean newTerm) {
		T term = null;
		Value<T> value = new Value<T>();
		Token incdec = null;

		if (token == INC || token == DEC) {
			incdec = token;
			match();
		}
		ThisOrSuper thisOrSuper = null;
		switch (token) {
		case NUMBER:
		case STRING:
		case HASH:
			term = generator.constant(constant());
			break;
		case L_CURLY:
			term = block();
			break;
		case DOT:
			term = generator.selfRef();
			// leave token == DOT
			thisOrSuper = ThisOrSuper.THIS;
			break;
		case L_PAREN:
			match(L_PAREN);
			term = expression();
			match(R_PAREN);
			break;
		case L_BRACKET:
			match(L_BRACKET);
			// TODO optimize literal part like cSuneido
			term = generator.functionCall(generator.identifier("Record"),
					value, argumentList(R_BRACKET));
			break;
		case IDENTIFIER:
			switch (lexer.getKeyword()) {
			case FUNCTION:
			case CLASS:
			case DLL:
			case STRUCT:
			case CALLBACK:
				term = generator.constant(constant());
				break;
			case TRUE:
			case FALSE:
				term = generator.constant(generator.bool(lexer.getKeyword() == TRUE));
				match();
				break;
			case THIS:
				term = generator.selfRef();
				thisOrSuper = ThisOrSuper.THIS;
				match();
				break;
			case SUPER:
				match(SUPER);
				if (incdec != null || (token != DOT && token != L_PAREN))
					syntaxError();
				term = generator.superRef();
				thisOrSuper = ThisOrSuper.SUPER;
				if (token == L_PAREN)
					value.member("_init", ThisOrSuper.SUPER);
				break;
			default:
				if (isGlobal(lexer.getValue()) &&
						lookAhead(! expectingCompound) == L_CURLY) {
					term = generator.constant(constant());
				} else {
					value.identifier(lexer.getValue());
					match(IDENTIFIER);
				}
			}
			break;
		default:
			syntaxError();
		}

		while (token == DOT || token == L_BRACKET || token == L_PAREN
				|| token == L_CURLY) {
			if (value.isSet() && (token == DOT || token == L_BRACKET)) {
				term = push(term, value);
				value.clear();
			}
			if (newTerm && token == L_PAREN) {
				if (value.isSet())
					term = push(term, value);
				return term;
			}
			if (token == DOT) {
				matchSkipNewlines(DOT);
				String id = lexer.getValue();
				match(IDENTIFIER);
				value.member(id, thisOrSuper);
				if (!expectingCompound && token == NEWLINE && lookAhead() == L_CURLY)
					match();
			} else if (matchIf(L_BRACKET)) {
				value.subscript(expression(), thisOrSuper);
				match(R_BRACKET);
			} else if (token == L_PAREN || token == L_CURLY) {
				if (value.isIdentifier()) {
					term = push(term, value);
					value.clear();
				} else if (value.isMember())
					generator.preFunctionCall(value);
				term = generator.functionCall(term, value, arguments());
				value.clear();
			}
			thisOrSuper = null;
		}

		if (incdec != null) {
			generator.lvalue(value);
			term = generator.preIncDec(term, incdec, value);
		} else if (assign()) {
			Token op = token;
			matchSkipNewlines();
			generator.lvalue(value);
			T expr = expression();
			term = generator.assignment(term, value, op, expr);
		} else if (token == INC || token == DEC) {
			generator.lvalue(value);
			term = generator.postIncDec(term, token, value);
			match();
		} else if (value.isSet()) {
			term = push(term, value);
		}
		return term;
	}

	private T push(T term, Value<T> value) {
		switch (value.type) {
		case IDENTIFIER:
			return generator.identifier(value.id);
		case MEMBER:
			return generator.member(term, value);
		case SUBSCRIPT:
			return generator.subscript(term, value.expr);
		}
		return null;
	}

	private boolean assign() {
		return (EQ_as_IS && token == EQ) ? false : token.assign();
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
		if (token == L_CURLY) {
			generator.argumentName("block");
			args = generator.argumentList(args, "block", block());
		}
		return args;
	}
	private T atArgument() {
		String n = "0";
		if (matchIf(ADD)) {
			n = lexer.getValue();
			match(NUMBER);
		}
		generator.atArgument(n);
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
				value = generator.constant(generator.bool(true));
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
		generator.argumentName(keyword);
		return keyword;
	}

	private T block() {
		Object loop = generator.startMethod(FuncOrBlock.BLOCK, null);
		match(L_CURLY);
		T params = token == BITOR ? blockParams() : null;
		generator.blockParams();
		T statements = statementList(loop);
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

	private T statementList(Object loop) {
		ParseFunction<T, G> p = new ParseFunction<T, G>(this);
		T result = p.statementList(loop);
		token = p.token;
		return result;
	}
}
