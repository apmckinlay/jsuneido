/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.util.List;

public interface Database {

	void close();

	Transaction readonlyTran();

	Transaction readwriteTran();

	long size();

	void addView(String name, String definition);

	void addTable(String tablename);

	boolean ensureTable(String tablename);

	void addColumn(String tablename, String column);

	void ensureColumn(String tablename, String column);

	void addIndex(String tablename, String columns, boolean isKey);

	void addIndex(String tablename, String columns, boolean isKey,
			boolean unique, String fktablename, String fkcolumns, int fkmode);

	void ensureIndex(String tablename, String columns, boolean isKey,
			boolean unique, String fktablename, String fkcolumns, int fkmode);

	void renameTable(String oldname, String newname);

	void renameColumn(String tablename, String oldname, String newname);

	boolean removeTable(String tablename);

	void removeColumn(String tablename, String column);

	void removeIndex(String tablename, String columns);

	String getSchema(String tablename);

	List<Integer> tranlist();

	void limitOutstandingTransactions();

	int finalSize();

	void force();

	void disableTrigger(String table);

	void enableTrigger(String table);

}