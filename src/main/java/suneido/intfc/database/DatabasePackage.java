/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public interface DatabasePackage {

	/** default database file name */
	String dbFilename();

	Database create(String filename);

	Database open(String filename);
	Database openReadonly(String filename);

	/** Creates an empty in-memory database */
	Database testdb();

	RecordBuilder recordBuilder();

	Record record(ByteBuffer buf);

	/**
	 * used when buf must be copied
	 * i.e. calling code will reuse buf
	 */
	Record recordCopy(ByteBuffer buf);

	Record record(int recadr, ByteBuffer buf);

	RecordStore recordStore();

	/** @return the number of tables dumped */
	int dumpDatabase(Database db, WritableByteChannel out);
	/** @return the number of records dumped */
	int dumpTable(Database db, String tablename, WritableByteChannel out);

	/** @return the number of tables loaded */
	int loadDatabase(Database db, ReadableByteChannel in);
	/** @return the number of records loaded */
	int loadTable(Database db, String tablename, ReadableByteChannel in);

	enum Status { OK, CORRUPTED, UNRECOVERABLE };
	interface Observer {
		void print(String msg);
	}
	Status check(String dbFilename, Observer ob);

	/** @return the number of tables copies */
	int compact(Database srcdb, Database dstdb);

	/** @return null if rebuild fails, otherwise result description */
	String rebuild(String dbFilename, String tempFilename);

	String rebuildFromData(String dbFilename, String tempFilename);

	Record minRecord();
	Record maxRecord();

	Observer printObserver = System.out::print;

	Observer nullObserver = (String msg) -> { };

	static class StringObserver implements Observer {
		StringBuilder sb = new StringBuilder();

		@Override
		public void print(String msg) {
			sb.append(msg);
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}

	/** used by Built() */
	String name();

	void renameDbWithBackup(String tempfile, String dbFilename);

	boolean dbExists(String dbFilename);

	void setOption(String name, Object value);

}
