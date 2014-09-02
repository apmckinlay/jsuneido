/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

public interface TableBuilder {

	TableBuilder ensureColumn(String column);

	TableBuilder addColumn(String column);

	TableBuilder renameColumn(String from, String to);

	TableBuilder dropColumn(String column);

	TableBuilder ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode);

	TableBuilder addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode);

	TableBuilder dropIndex(String columnNames);

	void finish();

	void abortUnfinished();

}