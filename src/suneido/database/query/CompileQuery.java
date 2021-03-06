/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import suneido.compiler.Lexer;
import suneido.compiler.ParseExpression;
import suneido.database.immudb.Transaction;
import suneido.database.query.expr.Expr;
import suneido.database.server.ServerData;
import suneido.database.immudb.Database;

public class CompileQuery {

	/** for tests */
	public static Query query(Database db, ServerData serverData, String s) {
		Transaction tran = db.readTransaction();
		try {
			return query(tran, serverData, s);
		} finally {
			tran.complete();
		}
	}

	public static Query query(Transaction t, ServerData serverData, String s,
			boolean is_cursor) {
		return parse(t, serverData, s).setup(is_cursor, t);
	}

	public static Query query(Transaction t, ServerData serverData, String s) {
		return parse(t, serverData, s).setup(t);
	}

	/** for tests */
	public static Query parse(Database db, ServerData serverData, String s) {
		Transaction tran = db.readTransaction();
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
				new ParseQuery<>(lexer, generator);
		pc.serverData(serverData);
		return (Query) pc.parse();
	}

	// for testing
	static Expr expr(String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		TreeQueryGenerator generator = new TreeQueryGenerator(null);
		ParseExpression<Object, QueryGenerator<Object>> pc =
				new ParseExpression<>(lexer,
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
