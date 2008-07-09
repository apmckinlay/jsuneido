package suneido.database.query;

import static suneido.database.Database.theDB;
import static suneido.database.Util.listToCommas;

import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import suneido.SuException;
import suneido.database.Table;
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

		public void ensure(String tablename, Schema schema) {
			Table table = theDB.ck_getTable(tablename);
			for (String col : schema.columns)
				if (!table.hasColumn(col))
					theDB.addColumn(tablename, col);
			for (Index index : schema.indexes) {
				String cols = listToCommas(index.columns);
				if (!table.hasIndex(cols))
					theDB.addIndex(tablename, cols,
						index.isKey, index.isUnique, false, index.in.table,
						listToCommas(index.in.columns), index.in.mode);
			}
		}

		public void alter_create(String table, Schema schema) {
			schema(table, schema);
		}

		public void alter_delete(String table, Schema schema) {
			for (String col : schema.columns)
				theDB.removeColumn(table, col);
			for (Index index : schema.indexes) {
				theDB.removeIndex(table, listToCommas(index.columns));
			}
		}

		private void schema(String table, Schema schema) {
			for (String col : schema.columns)
				theDB.addColumn(table, col);
			for (Index index : schema.indexes) {
				theDB.addIndex(table, listToCommas(index.columns),
						index.isKey, index.isUnique, false,
						index.in.table,
						listToCommas(index.in.columns), index.in.mode);
			}
		}

		public void alter_rename(String table, List<Rename> renames) {
			for (Rename r : renames)
				theDB.renameColumn(table, r.from, r.to);
		}

		public void rename(String from, String to) {
			theDB.renameTable(from, to);
		}

		public void drop(String table) {
			theDB.removeTable(table);
		}

	}

}

