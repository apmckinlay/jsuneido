package suneido.database.query;

import org.antlr.runtime.*;

import suneido.SuException;
import suneido.database.Transaction;
import suneido.database.query.expr.Expr;
import suneido.database.server.ServerData;

public class ParseQuery {

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
		QueryParser parser = parser(serverData, s);
		try {
			return parser.query();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	// for testing
	static Expr expr(String s) {
		QueryParser parser = parser(null, s);
		try {
			return parser.expression();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	private static QueryParser parser(ServerData serverData, String s) {
		ANTLRStringStream input = new ANTLRStringStream(s);
		QueryLexer lexer = new QueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		QueryParser parser = new QueryParser(tokens);
		parser.serverData = serverData;
		return parser;
	}

}
