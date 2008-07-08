package suneido.database.query;

import static suneido.database.Database.theDB;
import static suneido.database.query.Util.listToCommas;

import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import suneido.SuException;
import suneido.database.query.RequestParser.IRequest;
import suneido.database.query.RequestParser.Index;
import suneido.database.query.RequestParser.Rename;
import suneido.database.query.RequestParser.Schema;

/**
 * Parse and execute database "requests" to create, alter, or remove tables.
 * Uses the ANTLR grammer in {@Query.g}.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Request {
	public static void execute(String s) {
		ANTLRStringStream input = new ANTLRStringStream(s);
		RequestLexer lexer = new RequestLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		RequestParser parser = new RequestParser(tokens);
		parser.iRequest = new RequestImpl();
		try {
			parser.request();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	public static class RequestImpl implements IRequest {
		public void create(String table, Schema schema) {
			theDB.addTable(table);
			schema(table, schema);
		}

		public void ensure(String table, Schema schema) {
			System.out.println("ensure " + table);
			// schema(table, schema);
		}

		public void alter_create(String table, Schema schema) {
			schema(table, schema);
		}

		public void alter_delete(String table, Schema schema) {
			System.out.println("alter delete " + table);
			// schema(table, schema);
		}

		private void schema(String table, Schema schema) {
			if (schema.columns != null)
				for (String col : schema.columns)
					theDB.addColumn(table, col);
			for (Index index : schema.indexes) {
				theDB.addIndex(table, listToCommas(index.columns),
						index.isKey, index.isUnique, false, "", "", 0);
				// TODO foreign keys
				// if (index.in != null)
				// System.out.print(", " + index.in.table + ", "
				// + index.in.columns + ", " + index.in.mode);
				// System.out.println(")");
			}
		}

		public void alter_rename(String table, List<Rename> renames) {
			for (Rename r : renames)
				System.out.println("renameColumn(" + table + ", " + r.from
						+ ", " + r.to + ")");
		}

		public void rename(String from, String to) {
			System.out.println("renameTable(" + from + ", " + to + ")");
		}

		public void drop(String table) {
			// theDB.removeTable(table);
		}

	}

}

