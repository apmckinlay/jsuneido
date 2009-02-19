package suneido.language;

import static suneido.language.Token.COLON;
import static suneido.language.Token.Q_MARK;

public class ParseExpression<T> extends Parse<T> {
	ParseExpression(Lexer lexer, Generator<T> generator) {
		super(lexer, generator);
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
		} else
			return first;
	}

	private T orExpression() {
		return constant();
	}

	private T constant() {
		return new ParseConstant<T>(this).constant();
	}

}
