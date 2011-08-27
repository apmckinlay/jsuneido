/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.nio.ByteBuffer;

public interface DatabasePackage {

	String dbFilename();

	Database open(String filename);

	Database testdb();

	RecordBuilder recordBuilder();
	Record record(ByteBuffer buf);
	Record record(int recadr, ByteBuffer buf);

	void dumpDatabasePrint(String dbFilename, String outputFilename);
	int dumpDatabase(suneido.intfc.database.Database db, String outputFilename);

	void dumpTablePrint(String dbFilename, String tablename);
	int dumpTable(suneido.intfc.database.Database db, String tablename);

	void loadTablePrint(String actionArg);
	void loadDatabasePrint(String string, String string2) throws InterruptedException;

	void checkPrintExit(String dbFilename);

	void compactPrint(String dbFilename) throws InterruptedException;

	void rebuildOrExit(String dbFilename);

	Record minRecord();
	Record maxRecord();

}
