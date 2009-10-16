package suneido.database.query;

import suneido.database.Transaction;
import suneido.database.query.expr.Expr;
import suneido.database.server.ServerData;
import suneido.language.Lexer;
import suneido.language.ParseExpression;

public class CompileQuery {

	public static Query query(Transaction t, ServerData serverData, String s,
			boolean is_cursor) {
		return parse(serverData, s).setup(t, is_cursor);
	}

	public static Query query(Transaction t, ServerData serverData, String s) {
		return parse(serverData, s).setup(t);
	}

	public static Query parse(ServerData serverData, String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		TreeQueryGenerator generator = new TreeQueryGenerator();
		ParseQuery<Object, QueryGenerator<Object>> pc =
				new ParseQuery<Object, QueryGenerator<Object>>(lexer, generator);
		pc.serverData(serverData);
		return (Query) pc.parse();
	}

	// for testing
	static Expr expr(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		TreeQueryGenerator generator = new TreeQueryGenerator();
		ParseExpression<Object, QueryGenerator<Object>> pc =
				new ParseExpression<Object, QueryGenerator<Object>>(lexer,
						generator);
		pc.eq_as_is();
		return (Expr) pc.parse();
	}

	public static boolean isRequest(String query) {
		Lexer lexer = new Lexer(query);
		lexer.ignoreCase();
		lexer.nextSkipNewlines();
		switch (lexer.getKeyword()) {
		case INSERT:
		case UPDATE:
		case DELETE:
			return true;
		default:
			return false;
		}
	}

}
