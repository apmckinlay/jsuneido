/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import suneido.SuException;
import suneido.compiler.Lexer;
import suneido.database.server.ServerData;
import suneido.intfc.database.Database;
import suneido.intfc.database.TableBuilder;

/**
 * Parse and execute database "requests" to create, alter, or drop tables.
 */
@SuppressWarnings("unchecked")
public class Request implements RequestGenerator<Object> {
	private final Database db;
	private final ServerData serverData;

	public static void execute(Database db, String s) {
		Request.execute(db, null, s);
	}

	public static void execute(Database db, ServerData serverData, String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		Request generator = new Request(db, serverData);
		ParseRequest<Object> pc = new ParseRequest<>(lexer, generator);
		pc.parse();
	}

	private Request(Database db, ServerData serverData) {
		this.db = db;
		this.serverData = serverData;
	}

	@Override
	public Object columns(Object columns, String column) {
		List<String> list = columns == null
				? new ArrayList<String>() : (List<String>) columns;
		list.add(column);
		return list;
	}

	@Override
	public Object create(String table, Object schema) {
		if (! ((Schema) schema).hasKey())
			throw new SuException("key required for: " + table);
		alterCreate(db.createTable(table), (Schema) schema);
		return null;
	}

	@Override
	public Object alterCreate(String table, Object schema) {
		alterCreate(db.alterTable(table), (Schema) schema);
		return null;
	}

	private static void alterCreate(TableBuilder tb, Schema schema) {
		try {
			schema.create(tb);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
	}

	@Override
	public Object ensure(String tableName, Object schema) {
		TableBuilder tb = db.ensureTable(tableName);
		try {
			((Schema) schema).ensure(tb);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
		return null;
	}

	@Override
	public Object alterDrop(String tableName, Object schemaOb) {
		Schema schema = (Schema) schemaOb;
		TableBuilder tb = db.alterTable(tableName);
		try {
			// remove indexes first so columns aren't used
			for (AnIndex index : schema.indexes)
				tb.dropIndex(listToCommas(index.columns));
			for (String col : schema.columns)
				tb.dropColumn(col);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
		return null;
	}

	@Override
	public Object alterRename(String table, Object renames) {
		TableBuilder tb = db.alterTable(table);
		try {
			for (Rename r : (List<Rename>) renames)
				tb.renameColumn(r.from, r.to);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
		return null;
	}

	@Override
	public Object rename(String from, String to) {
		db.renameTable(from, to);
		return null;
	}

	@Override
	public Object view(String name, String definition) {
		db.addView(name, definition);
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
		if (serverData != null && serverData.getSview(table) != null)
			serverData.dropSview(table);
		else if (! db.dropTable(table))
			throw new SuException("can't drop nonexistent table: " + table);
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

		@Override
		public String toString() {
			if (table == null)
				return "";
			return " in " + table + " " + columns + mode;
		}
	}

	@Override
	public Object foreignKey(String table, Object columns, int mode) {
		return new ForeignKey(table, columns, mode);
	}

	private static class AnIndex {
		boolean key;
		boolean unique;
		List<String> columns;
		ForeignKey in;

		AnIndex(boolean key, boolean unique, Object columns, Object foreignKey) {
			this.key = key;
			this.unique = unique;
			this.columns = (List<String>) columns;
			this.in = (ForeignKey) foreignKey;
			if (in.table != null && in.columns == null)
				in.columns = (List<String>) columns;
		}

		void create(TableBuilder tb) {
			assert (in != null);
			tb.addIndex(listToCommas(columns), key, unique,
					in.table, listToCommas(in.columns), in.mode);
		}

		void ensure(TableBuilder tb) {
			assert (in != null);
			tb.ensureIndex(listToCommas(columns), key, unique,
					in.table, listToCommas(in.columns), in.mode);
		}

		@Override
		public String toString() {
			return (key ? "key" : ((unique ? "unique" : "") + " index")) +
					columns + in;
		}
	}

	@Override
	public Object index(boolean key, boolean unique, Object columns, Object foreignKey) {
		return new AnIndex(key, unique, columns,
				foreignKey == null ? new ForeignKey() : foreignKey);
	}

	@Override
	public Object indexes(Object indexes, Object index) {
		List<AnIndex> list = indexes == null
				? new ArrayList<AnIndex>() : (List<AnIndex>) indexes;
		list.add((AnIndex) index);
		return list;
	}

	static class Rename {
		String from;
		String to;

		Rename(String from, String to) {
			this.from = from;
			this.to = to;
		}
	}

	@Override
	public Object renames(Object renames, String from, String to) {
		List<Rename> list = renames == null
				? new ArrayList<Rename>() : (List<Rename>) renames;
		list.add(new Rename(from, to));
		return list;
	}

	@Override
	public Object schema(Object columns, Object indexes) {
		return new Schema(columns, indexes);
	}

	private static class Schema {
		List<String> columns;
		List<AnIndex> indexes;

		Schema(Object columns, Object indexes) {
			if (columns == null)
				columns = Collections.emptyList();
			this.columns = (List<String>) columns;
			if (indexes == null)
				indexes = Collections.emptyList();
			this.indexes = (List<AnIndex>) indexes;
		}
		void create(TableBuilder tb) {
			// add columns first so indexes can use them
			for (String column : columns)
				tb.addColumn(column);
			for (AnIndex index : indexes)
				index.create(tb);
		}
		void ensure(TableBuilder tb) {
			// add columns first so indexes can use them
			for (String col : columns)
				tb.ensureColumn(col);
			for (AnIndex index : indexes)
				index.ensure(tb);
		}
		boolean hasKey() {
			for (AnIndex index : indexes)
				if (index.key)
					return true;
			return false;
		}
	}

}
