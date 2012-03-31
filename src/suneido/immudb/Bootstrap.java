/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

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
	static final int[][] indexColumns =
			new int[][] { { }, { 0 }, { 0,2 }, { 0,1 } };

	static void create(ExclusiveTransaction t) {
		try {
			setup(t);
			create_tables(t);
			create_columns(t);
			create_indexes(t);
			TableBuilder.alter(t, "tables")
				.addIndex("tablename", true, false, null, null, 0)
				.build();
			TableBuilder.create(t, "views", TN.VIEWS)
				.addColumn("view_name")
				.addColumn("view_definition")
				.addIndex("view_name", true, false, null, null, 0)
				.build();
			t.complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	private static void setup(ExclusiveTransaction t) {
		t.addTableInfo(new TableInfo(TN.TABLES, 0, 0, 0, null));
		t.addTableInfo(new TableInfo(TN.COLUMNS, 0, 0, 0, null));
		t.addTableInfo(new TableInfo(TN.INDEXES, 0, 0, 0, null));
		t.addIndex(ReadTransaction.tables_index);
		t.addIndex(ReadTransaction.columns_index);
		t.addIndex(ReadTransaction.indexes_index);
	}

	private static void create_tables(ExclusiveTransaction t) {
		TableBuilder.create(t, "tables", TN.TABLES)
			.addColumn("table")
			.addColumn("tablename")
			.addIndex("table", true, false, null, null, 0)
			.build();
	}

	private static void create_columns(ExclusiveTransaction t) {
		TableBuilder.create(t, "columns", TN.COLUMNS)
			.addColumn("table")
			.addColumn("field")
			.addColumn("column")
			.addIndex("table,column", true, false, null, null, 0)
			.build();
	}

	private static void create_indexes(ExclusiveTransaction t) {
		TableBuilder.create(t, "indexes", TN.INDEXES)
			.addColumn("table")
			.addColumn("fields")
			.addColumn("key")
			.addColumn("fktable")
			.addColumn("fkcolumns")
			.addColumn("fkmode")
			.addIndex("table,fields", true, false, null, null, 0)
			.build();
	}

}
