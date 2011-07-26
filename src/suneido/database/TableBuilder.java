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
	public void ensureColumn(String column) {
		Schema.ensureColumn(db, tableName, column);
	}

	@Override
	public void addColumn(String column) {
		Schema.addColumn(db, tableName, column);
	}

	@Override
	public void renameColumn(String from, String to) {
		Schema.renameColumn(db, tableName, from, to);
	}

	@Override
	public void dropColumn(String column) {
		Schema.removeColumn(db, tableName, column);
	}

	@Override
	public void ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Schema.ensureIndex(db, tableName, columnNames, isKey, unique,
				fktable, fkcolumns, fkmode);
	}

	@Override
	public void addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Schema.addIndex(db, tableName, columnNames, isKey, unique,
				fktable, fkcolumns, fkmode);
	}

	@Override
	public void dropIndex(String columnNames) {
		Schema.removeIndex(db, tableName, columnNames);
	}

	@Override
	public void build() {
	}

	@Override
	public void finish() {
	}

	@Override
	public void abortUnfinished() {
	}

}
