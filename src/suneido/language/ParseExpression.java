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
			match(Q_MARK);
			T t = expression();
			match(COLON);
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
		// TODO new
		default:
			return term();
		}
	}

	private T term() {
		switch (token) {
		case IDENTIFIER:
			return matchReturn(IDENTIFIER, generator.identifier(value));
		default:
			return constant();
		}
	}

	private T constant() {
		// TODO move some of this to Parse method
		ParseConstant<T> p = new ParseConstant<T>(this);
		T result = p.constant();
		token = p.token;
		value = p.value;
		return result;
	}
}
