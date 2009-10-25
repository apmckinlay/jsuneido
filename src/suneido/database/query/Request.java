/*
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
package suneido.database.query;

import static suneido.database.Database.theDB;
import static suneido.util.Util.listToCommas;

import java.util.*;

import suneido.SuException;
import suneido.database.server.ServerData;
import suneido.language.Lexer;

/**
 * Parse and execute database "requests" to create, alter, or remove tables.
 *
 * @author Andrew McKinlay
 */
@SuppressWarnings("unchecked")
public class Request implements RequestGenerator<Object> {
	private final ServerData serverData;

	public Request(ServerData serverData) {
		this.serverData = serverData;
	}

	public static void execute(String s) {
		Request.execute(null, s);
	}

	public static void execute(ServerData serverData, String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		Request generator = new Request(serverData);
		ParseRequest<Object> pc = new ParseRequest<Object>(lexer, generator);
		pc.parse();
	}

	public Object columns(Object columns, String column) {
		List<String> list = columns == null
				? new ArrayList<String>() : (List<String>) columns;
		list.add(column);
		return list;
	}

	public Object create(String table, Object schemaOb) {
		Schema schema = (Schema) schemaOb;
		if (!schema.hasKey())
			throw new SuException("key required for: " + table);
		theDB.addTable(table);
		createSchema(table, schema);
		return null;
	}

	private void createSchema(String table, Schema schema) {
		for (String column : schema.columns)
			theDB.addColumn(table, column);
		for (Index index : schema.indexes)
			index.create(table);
	}


	public Object ensure(String tablename, Object schemaOb) {
		// TODO: should probably be all in one transaction
		Schema schema = (Schema) schemaOb;
		suneido.database.Table table = theDB.tables.get(tablename);
		if (table == null)
			create(tablename, schema);
		else {
			for (String col : schema.columns)
				if (!table.hasColumn(col))
					theDB.addColumn(tablename, col);
			for (Index index : schema.indexes) {
				String cols = listToCommas(index.columns);
				if (!table.hasIndex(cols))
					index.create(tablename);
			}
		}
		return null;
	}

	public Object alterCreate(String table, Object schema) {
		createSchema(table, (Schema) schema);
		return null;
	}

	public Object alterDrop(String table, Object schemaOb) {
		Schema schema = (Schema) schemaOb;
		for (String col : schema.columns)
			theDB.removeColumn(table, col);
		for (Index index : schema.indexes)
			theDB.removeIndex(table, listToCommas(index.columns));
		return null;
	}

	public Object alterRename(String table, Object renames) {
		for (Rename r : (List<Rename>) renames)
			r.rename(table);
		return null;
	}

	public Object rename(String from, String to) {
		theDB.renameTable(from, to);
		return null;
	}

	public Object view(String name, String definition) {
		theDB.add_view(name, definition);
		return null;
	}

	public Object sview(String name, String definition) {
		if (serverData.getSview(name) != null)
			throw new SuException("sview: '" + name + "' already exists");
		serverData.addSview(name, definition);
		return null;
	}

	public Object drop(String table) {
		if (serverData.getSview(table) != null)
			serverData.dropSview(table);
		else if (!theDB.removeTable(table))
			throw new SuException("nonexistent table: " + table);
		return null;
	}

	static class ForeignKey {
		String table;
		List<String> columns;
		int mode;

		ForeignKey(String table, Object columns, int mode) {
			this.table = table;
			this.columns = (List<String>) columns;
			this.mode = mode;
		}
		ForeignKey() {
		}
	}

	public Object foreignKey(String table, Object columns, int mode) {
		return new ForeignKey(table, columns, mode);
	}

	static class Index {
		boolean key;
		boolean unique;
		boolean lower;
		List<String> columns;
		ForeignKey in;

		Index(boolean key, boolean unique, boolean lower, Object columns,
				Object foreignKey) {
			this.key = key;
			this.unique = unique;
			this.lower = lower;
			this.columns = (List<String>) columns;
			this.in = (ForeignKey) foreignKey;
		}

		void create(String table) {
			assert (in != null);
			theDB.addIndex(table, listToCommas(columns), key, unique, lower,
					in.table, listToCommas(in.columns), in.mode);
		}
	}

	public Object index(boolean key, boolean unique, boolean lower,
			Object columns, Object foreignKey) {
		return new Index(key, unique, lower, columns, foreignKey == null
				? new ForeignKey() : foreignKey);
	}

	public Object indexes(Object indexes, Object index) {
		List<Index> list = indexes == null
				? new ArrayList<Index>() : (List<Index>) indexes;
		list.add((Index) index);
		return list;
	}

	static class Rename {
		String from;
		String to;

		Rename(String from, String to) {
			this.from = from;
			this.to = to;
		}
		void rename(String table) {
			theDB.renameColumn(table, from, to);
		}
	}

	public Object renames(Object renames, String from, String to) {
		List<Rename> list = renames == null
				? new ArrayList<Rename>() : (List<Rename>) renames;
		list.add(new Rename(from, to));
		return list;
	}

	static class Schema {
		List<String> columns;
		List<Index> indexes;

		Schema(Object columns, Object indexes) {
			if (columns == null)
				columns = Collections.emptyList();
			this.columns = (List<String>) columns;
			if (indexes == null)
				indexes = Collections.emptyList();
			this.indexes = (List<Index>) indexes;
		}
		boolean hasKey() {
			for (Index index : indexes)
				if (index.key)
					return true;
			return false;
		}
	}

	public Object schema(Object columns, Object indexes) {
		return new Schema(columns, indexes);
	}

}

