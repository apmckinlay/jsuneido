package suneido.database.query;

import static suneido.database.Database.theDB;
import suneido.database.Transaction;
import suneido.database.query.expr.Expr;
import suneido.database.server.ServerData;
import suneido.language.Lexer;
import suneido.language.ParseExpression;

public class CompileQuery {

	/** for tests */
	public static Query query(ServerData serverData, String s) {
		Transaction tran = theDB.readonlyTran();
		try {
			return query(tran, serverData, s);
		} finally {
			tran.complete();
		}
	}

	public static Query query(Transaction t, ServerData serverData, String s,
			boolean is_cursor) {
		return parse(t, serverData, s).setup(is_cursor);
	}

	public static Query query(Transaction t, ServerData serverData, String s) {
		return parse(t, serverData, s).setup();
	}

	/** for tests */
	public static Query parse(ServerData serverData, String s) {
		Transaction tran = theDB.readonlyTran();
		try {
			return parse(tran, serverData, s);
		} finally {
			tran.complete();
		}
	}

	public static Query parse(Transaction tran, ServerData serverData, String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		TreeQueryGenerator generator = new TreeQueryGenerator(tran);
		ParseQuery<Object, QueryGenerator<Object>> pc =
				new ParseQuery<Object, QueryGenerator<Object>>(lexer, generator);
		pc.serverData(serverData);
		return (Query) pc.parse();
	}

	// for testing
	static Expr expr(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		TreeQueryGenerator generator = new TreeQueryGenerator(null);
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
