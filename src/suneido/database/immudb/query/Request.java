/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.query;

import static suneido.util.Util.listToCommas;

import java.util.*;

import suneido.SuException;
import suneido.database.immudb.Database;
import suneido.database.immudb.TableBuilder;
import suneido.database.query.ParseRequest;
import suneido.database.query.RequestGenerator;
import suneido.language.Lexer;

/**
 * Parse and execute database "requests" to create, alter, or remove tables.
 */
@SuppressWarnings("unchecked")
public class Request implements RequestGenerator<Object> {
	private final Database db;

	private Request(Database db) {
		this.db = db;
	}

	public static void execute(Database db, String s) {
		Lexer lexer = new Lexer(s);
		lexer.ignoreCase();
		Request generator = new Request(db);
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
		createSchema(table, schema);
		return null;
	}

	private void createSchema(String table, Schema schema) {
		TableBuilder tb = db.createTable(table);
		try {
			schema(schema, tb);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
	}

	private void schema(Schema schema, TableBuilder tb) {
		for (String column : schema.columns)
			tb.addColumn(column);
		for (Index index : schema.indexes)
			index.create(tb);
	}

	@Override
	public Object alterCreate(String table, Object schema) {
		alterSchema(table, (Schema) schema);
		return null;
	}

	private void alterSchema(String table, Schema schema) {
		TableBuilder tb = db.alterTable(table);
		try {
			schema(schema, tb);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
	}

	@Override
	public Object ensure(String tableName, Object schemaOb) {
		Schema schema = (Schema) schemaOb;
		TableBuilder tb = db.ensureTable(tableName);
		try {
			for (String col : schema.columns)
				tb.ensureColumn(col);
			for (Index index : schema.indexes)
				index.ensure(tb);
			tb.finish();
		} finally {
			tb.abortUnfinished();
		}
		return null;
	}

	@Override
	public Object alterDrop(String table, Object schemaOb) {
//		Schema schema = (Schema) schemaOb;
//		for (String col : schema.columns)
//			TheDb.db().removeColumn(table, col);
//		for (Index index : schema.indexes)
//			TheDb.db().removeIndex(table, listToCommas(index.columns));
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
//		TheDb.db().renameTable(from, to);
		return null;
	}

	@Override
	public Object view(String name, String definition) {
//		TheDb.db().add_view(name, definition);
		return null;
	}

	@Override
	public Object sview(String name, String definition) {
//		if (serverData.getSview(name) != null)
//			throw new SuException("sview: '" + name + "' already exists");
//		serverData.addSview(name, definition);
		return null;
	}

	@Override
	public Object drop(String table) {
//		if (serverData.getSview(table) != null)
//			serverData.dropSview(table);
//		else if (!TheDb.db().removeTable(table))
//			throw new SuException("nonexistent table: " + table);
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

	class Index {
		boolean key;
		boolean unique;
		List<String> columns;
		ForeignKey in;

		Index(boolean key, boolean unique, Object columns, Object foreignKey) {
			this.key = key;
			this.unique = unique;
			this.columns = (List<String>) columns;
			this.in = (ForeignKey) foreignKey;
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
	}

	@Override
	public Object index(boolean key, boolean unique, Object columns, Object foreignKey) {
		return new Index(key, unique, columns,
				foreignKey == null ? new ForeignKey() : foreignKey);
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
//			TheDb.db().renameColumn(table, from, to);
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
