/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

class TableBuilder implements suneido.intfc.database.TableBuilder {
	private final Database db;
	private final String tableName;

	static TableBuilder create(Database db, String tablename) {
		TableBuilder tb = new TableBuilder(db, tablename);
		tb.createTable();
		return tb;
	}

	static TableBuilder alter(Database db, String tableName) {
		return new TableBuilder(db, tableName);
	}

	private TableBuilder(Database db, String tblname) {
		this.db = db;
		this.tableName = tblname;
	}

	private void createTable() {
		Schema.addTable(db, tableName);
	}

	@Override
	public TableBuilder ensureColumn(String column) {
		Schema.ensureColumn(db, tableName, column);
		return this;
	}

	@Override
	public TableBuilder addColumn(String column) {
		Schema.addColumn(db, tableName, column);
		return this;
	}

	@Override
	public TableBuilder renameColumn(String from, String to) {
		Schema.renameColumn(db, tableName, from, to);
		return this;
	}

	@Override
	public TableBuilder dropColumn(String column) {
		Schema.removeColumn(db, tableName, column);
		return this;
	}

	@Override
	public TableBuilder ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Schema.ensureIndex(db, tableName, columnNames, isKey, unique,
				fktable, fkcolumns, fkmode);
		return this;
	}

	@Override
	public TableBuilder addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Schema.addIndex(db, tableName, columnNames, isKey, unique,
				fktable, fkcolumns, fkmode);
		return this;
	}

	@Override
	public TableBuilder dropIndex(String columnNames) {
		Schema.removeIndex(db, tableName, columnNames);
		return this;
	}

	@Override
	public void finish() {
	}

	@Override
	public void abortUnfinished() {
	}

}
