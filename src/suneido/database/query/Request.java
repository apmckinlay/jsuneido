/* Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.listToCommas;

import java.util.*;

import suneido.SuException;
import suneido.database.TheDb;
import suneido.database.server.ServerData;
import suneido.language.Lexer;

/**
 * Parse and execute database "requests" to create, alter, or remove tables.
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

	@Override
	public Object columns(Object columns, String column) {
		List<String> list = columns == null
				? new ArrayList<String>() : (List<String>) columns;
		list.add(column);
		return list;
	}

	@Override
	public Object create(String table, Object schemaOb) {
		Schema schema = (Schema) schemaOb;
		if (!schema.hasKey())
			throw new SuException("key required for: " + table);
		TheDb.db().addTable(table);
		createSchema(table, schema);
		return null;
	}

	private void createSchema(String table, Schema schema) {
		for (String column : schema.columns)
			TheDb.db().addColumn(table, column);
		for (Index index : schema.indexes)
			index.create(table);
	}


	@Override
	public Object ensure(String tablename, Object schemaOb) {
		// TODO should probably be all in one transaction
		Schema schema = (Schema) schemaOb;
		if (TheDb.db().ensureTable(tablename))
			createSchema(tablename, schema);
		else {
			for (String col : schema.columns)
					TheDb.db().ensureColumn(tablename, col);
			for (Index index : schema.indexes)
				index.ensure(tablename);
		}
		return null;
	}

	@Override
	public Object alterCreate(String table, Object schema) {
		createSchema(table, (Schema) schema);
		return null;
	}

	@Override
	public Object alterDrop(String table, Object schemaOb) {
		Schema schema = (Schema) schemaOb;
		for (String col : schema.columns)
			TheDb.db().removeColumn(table, col);
		for (Index index : schema.indexes)
			TheDb.db().removeIndex(table, listToCommas(index.columns));
		return null;
	}

	@Override
	public Object alterRename(String table, Object renames) {
		for (Rename r : (List<Rename>) renames)
			r.rename(table);
		return null;
	}

	@Override
	public Object rename(String from, String to) {
		TheDb.db().renameTable(from, to);
		return null;
	}

	@Override
	public Object view(String name, String definition) {
		TheDb.db().add_view(name, definition);
		return null;
	}

	@Override
	public Object sview(String name, String definition) {
		if (serverData.getSview(name) != null)
			throw new SuException("sview: '" + name + "' already exists");
		serverData.addSview(name, definition);
		return null;
	}

	@Override
	public Object drop(String table) {
		if (serverData.getSview(table) != null)
			serverData.dropSview(table);
		else if (!TheDb.db().removeTable(table))
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

	@Override
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
			TheDb.db().addIndex(table, listToCommas(columns), key, unique, lower,
					in.table, listToCommas(in.columns), in.mode);
		}

		void ensure(String table) {
			assert (in != null);
			TheDb.db().ensureIndex(table, listToCommas(columns), key, unique, lower,
					in.table, listToCommas(in.columns), in.mode);
		}
	}

	@Override
	public Object index(boolean key, boolean unique, boolean lower,
			Object columns, Object foreignKey) {
		return new Index(key, unique, lower, columns, foreignKey == null
				? new ForeignKey() : foreignKey);
	}

	@Override
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
			TheDb.db().renameColumn(table, from, to);
		}
	}

	@Override
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

	@Override
	public Object schema(Object columns, Object indexes) {
		return new Schema(columns, indexes);
	}

}

