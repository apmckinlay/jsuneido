/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

public interface TableBuilder {

	void ensureColumn(String column);

	void addColumn(String column);

	void renameColumn(String from, String to);

	void dropColumn(String column);

	void ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode);

	void addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode);

	void dropIndex(String columnNames);

	void build();

	void finish();

	void abortUnfinished();

}