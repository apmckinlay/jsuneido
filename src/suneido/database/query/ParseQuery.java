package suneido.database.query;

import org.antlr.runtime.*;

import suneido.SuException;
import suneido.database.query.expr.Expr;

public class ParseQuery {
	public static Query parse(String s) {
		QueryParser parser = setup(s);
		try {
			return parser.query();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	// for testing
	static Expr expr(String s) {
		QueryParser parser = setup(s);
		try {
			return parser.expression();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	private static QueryParser setup(String s) {
		ANTLRStringStream input = new ANTLRStringStream(s);
		QueryLexer lexer = new QueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		QueryParser parser = new QueryParser(tokens);
		return parser;
	}

}
