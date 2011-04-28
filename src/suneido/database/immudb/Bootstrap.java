/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;


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
public class Bootstrap {
	public static class TN
		{ public final static int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4; }

	public static void create(UpdateTransaction t) {
		t.addIndex(TN.TABLES, "0");
		t.addIndex(TN.COLUMNS, "0,1");
		t.addIndex(TN.INDEXES, "0,1");

		TableBuilder tb = TableBuilder.builder(t, "tables", TN.TABLES);
		tb.addColumn("table");
		tb.addColumn("tablename");
		tb.addIndex("table", true, false, null, null, 0);
		tb.addIndex("tablename", true, false, null, null, 0);
		tb.build();

		tb = TableBuilder.builder(t, "columns", TN.COLUMNS);
		tb.addColumn("table");
		tb.addColumn("field");
		tb.addColumn("column");
		tb.addIndex("table,field", true, false, null, null, 0);
		tb.build();

		tb = TableBuilder.builder(t, "indexes", TN.INDEXES);
		tb.addColumn("table");
		tb.addColumn("columns");
		tb.addColumn("key");
		tb.addColumn("fktable");
		tb.addColumn("fkcolumns");
		tb.addColumn("fkmode");
		tb.addIndex("table,columns", true, false, null, null, 0);
		tb.build();

		tb = TableBuilder.builder(t, "views", TN.VIEWS);
		tb.addColumn("view_name");
		tb.addColumn("view_definition");
		tb.addIndex("view_name", true, false, null, null, 0);
		tb.build();

		t.commit();
	}

}
