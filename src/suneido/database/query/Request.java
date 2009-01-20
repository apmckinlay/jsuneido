package suneido.database.query;

import static suneido.Util.listToCommas;
import static suneido.database.Database.theDB;

import java.util.List;

import org.antlr.runtime.*;

import suneido.SuException;
import suneido.database.Table;
import suneido.database.query.RequestParser.*;
import suneido.database.query.RequestParser.Rename;
import suneido.database.server.ServerData;

/**
 * Parse and execute database "requests" to create, alter, or remove tables.
 * Uses the ANTLR grammer in {Request.g}.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Request {
	public static void execute(String s) {
		Request.execute(null, s);
	}

	public static void execute(ServerData serverData, String s) {
		ANTLRStringStream input = new ANTLRStringStream(s);
		RequestLexer lexer = new RequestLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		RequestParser parser = new RequestParser(tokens);
		parser.iRequest = new RequestImpl(serverData);
		try {
			parser.request();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	public static class RequestImpl implements IRequest {
		private final ServerData serverData;

		public RequestImpl(ServerData serverData) {
			this.serverData = serverData;
		}

		public void create(String table, Schema schema) {
			if (!hasKey(schema))
				throw new SuException("key required for: " + table);
			theDB.addTable(table);
			schema(table, schema);
		}

		private boolean hasKey(Schema schema) {
			for (Index index : schema.indexes)
				if (index.isKey)
					return true;
			return false;
		}

		public void ensure(String tablename, Schema schema) {
			Table table = theDB.getTable(tablename);
			if (table == null)
				create(tablename, schema);
			else {
				for (String col : schema.columns)
					if (!table.hasColumn(col))
						theDB.addColumn(tablename, col);
				for (Index index : schema.indexes) {
					String cols = listToCommas(index.columns);
					if (!table.hasIndex(cols))
						theDB.addIndex(tablename, cols, index.isKey,
								index.isUnique, false, index.in.table,
								listToCommas(index.in.columns), index.in.mode);
				}
			}
		}

		public void alter_create(String table, Schema schema) {
			schema(table, schema);
		}

		public void alter_delete(String table, Schema schema) {
			for (String col : schema.columns)
				theDB.removeColumn(table, col);
			for (Index index : schema.indexes)
				theDB.removeIndex(table, listToCommas(index.columns));
		}

		private void schema(String table, Schema schema) {
			for (String col : schema.columns)
				theDB.addColumn(table, col);
			for (Index index : schema.indexes)
				theDB.addIndex(table, listToCommas(index.columns),
						index.isKey, index.isUnique, false,
						index.in.table,
						listToCommas(index.in.columns), index.in.mode);
		}

		public void alter_rename(String table, List<Rename> renames) {
			for (Rename r : renames)
				theDB.renameColumn(table, r.from, r.to);
		}

		public void rename(String from, String to) {
			theDB.renameTable(from, to);
		}

		public void drop(String table) {
			if (serverData.getSview(table) != null)
				serverData.dropSview(table);
			else if (theDB.getView(table) != null)
				theDB.removeView(table);
			else
				theDB.removeTable(table);
		}

		public void view(String name, String definition) {
			checkExisting(name);
			theDB.add_view(name, definition);
		}

		public void sview(String name, String definition) {
			checkExisting(name);
			serverData.addSview(name, definition);
		}

		private void checkExisting(String name) {
			if (theDB.getView(name) != null || serverData.getSview(name) != null)
				throw new SuException("view: '" + name + "' already exists");
		}

		public void error(String msg) {
			throw new SuException("syntax error: " + msg);
		}

	}

}

