/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static suneido.compiler.ParseFunction.LOOP;
import static suneido.compiler.Token.*;

import java.util.Set;

import com.google.common.base.Ascii;
import com.google.common.collect.Sets;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.compiler.ParseFunction.Context;
import suneido.database.query.ParseQuery;
import suneido.runtime.Numbers;

public class ParseExpression<T, G extends Generator<T>> extends Parse<T, G> {
	boolean EQ_as_IS = false;
	private boolean inQuery = false;

	public ParseExpression(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	public ParseExpression(Parse<T, G> parse) {
		super(parse);
		if (parse instanceof ParseQuery)
			inQuery = true;
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
		if (token == OR) {
			result = generator.or(null, result);
			while (token == OR) {
				matchSkipNewlines();
				result = generator.or(result, andExpression());
			}
			result = generator.orEnd(result);
		}
		return result;
	}

	private T andExpression() {
		T result = inExpression();
		if (token == AND) {
			result = generator.and(null, result);
			do {
				matchSkipNewlines();
				result = generator.and(result, inExpression());
			} while (token == AND);
			result = generator.andEnd(result);
		}
		return result;
	}

	private T inExpression() {
		T expr = bitorExpression();
		Token op = token;
		if (isIn()) {
			matchSkipNewlines();
			T exprs = null;
			match(L_PAREN);
			while (token != R_PAREN) {
				exprs = generator.expressionList(exprs, expression());
				matchIf(COMMA);
			}
			match(R_PAREN);
			T e = generator.in(expr, exprs);
			if (op == NOT)
				e = generator.unaryExpression(NOT, e);
			return e;
		}
		return expr;
	}

	private boolean isIn() {
		if (token == NOT && lookAhead() == IDENTIFIER && ahead.getKeyword() == IN)
			match();
		return lexer.getKeyword() == IN;
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
		while (token == IS || token == ISNT ||
				token == MATCH || token == MATCHNOT ||
				(EQ_as_IS && token == EQ)) {
			Token op = (token == EQ ? IS : token);
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
			return lexer.getKeyword() == NEW ? newExpression() : term();
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
		Token preincdec = null;
		if (token == INC || token == DEC) {
			preincdec = token;
			match();
		}
		T term = primary();
		while (token == DOT || token == L_BRACKET || token == L_PAREN ||
				(token == L_CURLY && ! expectingCompound)) {
			if (newTerm && token == L_PAREN)
				return term;
			if (token == DOT) {
				matchSkipNewlines(DOT);
				int lineNumber = lexer.getLineNumber();
				String id = lexer.getValue();
				match(IDENTIFIER);
				term = generator.memberRef(term, id, lineNumber);
				if (!expectingCompound && token == NEWLINE && lookAhead() == L_CURLY)
					match();
			} else if (matchIf(L_BRACKET)) {
				T expr;
				if (token == RANGETO || token == RANGELEN) {
					expr = generator.value(0);
				} else {
					expr = expression();
				}
				if (token == RANGETO || token == RANGELEN) {
					Token type = token;
					match();
					T expr2;
					if (R_BRACKET == token) {
						expr2 = generator.value(Integer.MAX_VALUE);
					} else {
						expr2 = expression();
					}
					expr = generator.range(type, expr, expr2);
				}
				term = generator.subscript(term, expr);
				match(R_BRACKET);
			} else if (token == L_PAREN || token == L_CURLY) {
				term = generator.functionCall(term, arguments());
			}
		}
		if (preincdec != null)
			term = generator.preIncDec(term, preincdec);
		else if (assign()) {
			Token op = token;
			matchSkipNewlines();
			T expr = expression();
			term = generator.assignment(term, op, expr);
		} else if (token == INC || token == DEC) {
			term = generator.postIncDec(term, token);
			match();
		}
		return term;
	}

	private T primary() {
		switch (token) {
		case STRING:
			// don't call constant because it allows concatenation
			return matchReturn(generator.value(lexer.getValue()));
		case NUMBER:
		case HASH:
			return constant();
		case L_CURLY:
			return block();
		case DOT:
			// note: DOT not matched
			return generator.selfRef(lexer.getLineNumber());
		case L_PAREN:
			match(L_PAREN);
			return matchReturn(R_PAREN, generator.rvalue(expression()));
		case L_BRACKET:
			if (inQuery)
				return constant();
			else {
				int lineNumber = lexer.getLineNumber();
				match(L_BRACKET);
				T func = generator.identifier("[", lineNumber);
				return generator.functionCall(func, argumentList(R_BRACKET));
			}
		case IDENTIFIER:
			return primaryIdentifier();
		default:
			syntaxError();
			return null; // unreachable
		}
	}

	private T primaryIdentifier() {
		switch (lexer.getKeyword()) {
		case FUNCTION:
		case CLASS:
			return constant();
		case TRUE:
			return matchReturn(generator.value(true));
		case FALSE:
			return matchReturn(generator.value(false));
		case SUPER:
			return superCall();
		default:
			String id = lexer.getValue();
			if (id.equals("dll") || id.equals("callback") || id.equals("struct"))
				syntaxError("jSuneido does not implement " + id);
			if (isGlobal(id) && lookAhead(! expectingCompound) == L_CURLY)
				return constant(); // Base{ => class
			else {
				int lineNumber = lexer.getLineNumber();
				match(IDENTIFIER);
				if (matchIf(AT)) {
					id += "@" + lexer.getValue();
					match(IDENTIFIER);
				}
				return generator.identifier(id, lineNumber);
			}
		}
	}

	private T superCall() {
		int lineNumber = lexer.getLineNumber();
		match(SUPER);
		if (token == L_PAREN)
			return generator.superCallTarget("New", lineNumber);
		if (token != DOT)
			syntaxError("invalid use of super");
		match();
		if (token != IDENTIFIER)
			syntaxError("invalid use of super");
		lineNumber = lexer.getLineNumber();
		String method = lexer.getValue();
		match();
		if (token != L_PAREN)
			syntaxError("invalid use of super");
		return generator.superCallTarget(method, lineNumber);
	}


	private boolean assign() {
		return (!EQ_as_IS || token != EQ) && token.assign();
	}
	private static boolean isGlobal(String name) {
		int i = name.startsWith("_") && name.length() > 1 ? 1 : 0;
		return Ascii.isUpperCase(name.charAt(i));
	}
	private T arguments() {
		T args = null;
		if (matchIf(L_PAREN)) {
			if (matchIf(AT))
				return atArgument();
			else
				args = argumentList(R_PAREN);
		}
		while (token == NEWLINE && !expectingCompound && lookAhead() == L_CURLY)
			match();
		if (token == L_CURLY) {
			args = generator.argumentList(args, generator.value("block"), block());
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
		T keyword = null;
		Set<String> keywords = Sets.newHashSet();
		while (token != closing) {
			T expr;
			if (token == COLON) {
				// f(:name) is equivalent to f(name: name)
				int lineNumber = lexer.getLineNumber();
				match(COLON);
				String identifier = lexer.getValue();
				if (!Ascii.isLowerCase(identifier.charAt(0)))
					syntaxError("expecting local variable name");
				if (! keywords.add(identifier)) //FIXME e.g. '1' and '01'
					throw new SuException("duplicate argument name: " + identifier);
				match(IDENTIFIER);
				keyword = generator.value(identifier);
				expr = generator.identifier(identifier, lineNumber);
			} else {
				if (isKeyword()) {
					if (! keywords.add(lexer.getValue()))
						throw new SuException("duplicate argument name: " +
								lexer.getValue());
					keyword = keyword();
				} else if (keywords.size() > 0)
					syntaxError("un-named arguments must come before named arguments");

				boolean trueDefault = (keyword != null &&
						(token == COMMA || token == closing || isKeyword()));

				expr = trueDefault
						? generator.value(true)
						: expression();
			}
			args = generator.argumentList(args, keyword, expr);
			matchIf(COMMA);
		}
		match(closing);
		return args;
	}
	private boolean isKeyword() {
		return (token == STRING || token == IDENTIFIER || token == NUMBER) &&
				lookAhead() == COLON;
	}
	private T keyword() {
		T keyword;
		if (token == STRING || token == IDENTIFIER)
			keyword = generator.value(lexer.getValue());
		else if (token == NUMBER)
			keyword = generator.value(Numbers.stringToNumber(lexer.getValue()));
		else
			throw new SuInternalError("invalid keyword");
		match();
		match(COLON);
		return keyword;
	}

	private T block() {
		int lineNumber = lexer.getLineNumber();
		match(L_CURLY);
		T params = token == BITOR ? blockParams() : null;
		T statements = statementList(LOOP);
		match(R_CURLY);
		return generator.block(params, statements, lineNumber);
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
		ParseConstant<T, G> p = new ParseConstant<>(this);
		T result = p.constant();
		token = p.token;
		return result;
	}

	private T statementList(Context context) {
		ParseFunction<T, G> p = new ParseFunction<>(this);
		T result = p.statementList(context);
		token = p.token;
		return result;
	}

}
