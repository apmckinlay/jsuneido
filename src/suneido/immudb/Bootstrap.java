/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.database.query.Request;


/**
 * Create a new database with the initial schema:
 * 	tables (table, tablename)
 * 		key(table) key(tablename)
 * 	columns (table, field, column)
 * 		key(table, field)
 * 	indexes (table, columns, key, fktable, fkcolumns, fkmode)
 *		key(table, columns)
 *	views (view_name, view_definition)
 *		key(view_name)
 */
class Bootstrap {
	static class TN
		{ static final int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4; }

	static void create(Database db) {
		UpdateTransaction t = db.readwriteTran();
		setup(t);
		create_tables(t);
		create_columns(t);
		create_indexes(t);
		t.complete();
		Request.execute(db,
				"alter tables create key(tablename)");
		Request.execute(db,
				"create views (view_name, view_definition) key(view_name)");
	}

	private static void setup(UpdateTransaction t) {
		t.addTableInfo(new TableInfo(TN.TABLES, 0, 0, 0, null));
		t.addTableInfo(new TableInfo(TN.COLUMNS, 0, 0, 0, null));
		t.addTableInfo(new TableInfo(TN.INDEXES, 0, 0, 0, null));
		t.addIndex(TN.TABLES, "0");
		t.addIndex(TN.COLUMNS, "0,1");
		t.addIndex(TN.INDEXES, "0,1");
	}

	private static void create_tables(UpdateTransaction t) {
		TableBuilder tb = TableBuilder.create(t, "tables", TN.TABLES);
		tb.addColumn("table");
		tb.addColumn("tablename");
		tb.addIndex("table", true, false, null, null, 0);
		tb.build();
	}

	private static void create_columns(UpdateTransaction t) {
		TableBuilder tb;
		tb = TableBuilder.create(t, "columns", TN.COLUMNS);
		tb.addColumn("table");
		tb.addColumn("field");
		tb.addColumn("column");
		tb.addIndex("table,field", true, false, null, null, 0);
		tb.build();
	}

	private static void create_indexes(UpdateTransaction t) {
		TableBuilder tb;
		tb = TableBuilder.create(t, "indexes", TN.INDEXES);
		tb.addColumn("table");
		tb.addColumn("columns");
		tb.addColumn("key");
		tb.addColumn("fktable");
		tb.addColumn("fkcolumns");
		tb.addColumn("fkmode");
		tb.addIndex("table,columns", true, false, null, null, 0);
		tb.build();
	}

}
