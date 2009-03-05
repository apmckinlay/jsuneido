package suneido.database.query;

import suneido.database.Transaction;
import suneido.database.query.expr.Expr;
import suneido.database.server.ServerData;
import suneido.language.Lexer;
import suneido.language.ParseExpression;

public class CompileQuery {

	public static Query query(ServerData serverData, String s) {
		return query(serverData, s, false);
	}

	public static Query query(ServerData serverData, String s, boolean is_cursor) {
		return parse(serverData, s).setup(is_cursor);
	}

	public static Query query(ServerData serverData, String s, Transaction tran) {
		Query q = parse(serverData, s).setup();
		q.setTransaction(tran);
		return q;
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

}
