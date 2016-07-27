/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.util.List;

public interface Database extends AutoCloseable {

	Transaction readTransaction();

	Transaction updateTransaction();

	long size();

	void addView(String name, String definition);

	void renameTable(String oldname, String newname);

	boolean dropTable(String tablename);

	String getSchema(String tablename);

	List<Integer> tranlist();

	void limitOutstandingTransactions();

	int finalSize();

	void force();

	void disableTrigger(String table);

	void enableTrigger(String table);

	TableBuilder createTable(String tableName);

	TableBuilder alterTable(String table);

	TableBuilder ensureTable(String tableName);

	/** used for tests */
	Database reopen();

	@Override
	void close();

	void checkTransEmpty();

	/** @return "" if successful, otherwise an error message */
	String check();

}
