/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.nio.ByteBuffer;


public interface DatabasePackage {

	Database open(String filename);

	Database testdb();

	int offsetToInt(long offset);

	long intToOffset(int i);

	Record record();
	Record record(int size);
	Record record(ByteBuffer buf);

	void dumpDatabasePrint(String db_filename, String output_filename);
	int dumpDatabase(suneido.Database db, String output_filename);

	void dumpTablePrint(String db_filename, String tablename);
	int dumpTable(suneido.Database db, String tablename);

	void checkPrintExit(String db_filename);

	void compactPrint(String db_filename) throws InterruptedException;
	void compact2(String db_filename, String tempfilename);

	void rebuildOrExit(String db_filename);
	void rebuild2(String db_filename, String tempfilename);

	void loadTablePrint(String actionArg);
	void loadDatabasePrint(String string, String string2) throws InterruptedException;
	void load2(String string, String actionArg);

}
