/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public interface DatabasePackage {

	String dbFilename();

	Database create(String filename);

	Database open(String filename);
	Database openReadonly(String filename);

	Database testdb();

	RecordBuilder recordBuilder();
	Record record(ByteBuffer buf);
	Record record(int recadr, ByteBuffer buf);

	int dumpDatabase(Database db, WritableByteChannel out);
	int dumpTable(Database db, String tablename, WritableByteChannel out);

	int loadDatabase(Database db, ReadableByteChannel in);
	int loadTable(Database db, String tablename, ReadableByteChannel in);

	/** for tests */ void check(String dbFilename);
	void checkPrintExit(String dbFilename);

	/** for tests */ void compact(String dbFilename, String tempfilename);
	void compactPrint(String dbFilename) throws InterruptedException;

	/** for tests*/ void rebuild(String dbFilename, String tempfilename);
	void rebuildOrExit(String dbFilename);

	Record minRecord();
	Record maxRecord();

}
