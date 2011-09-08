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

	Database testdb();

	RecordBuilder recordBuilder();
	Record record(ByteBuffer buf);
	Record record(int recadr, ByteBuffer buf);

	/** @return the number of tables dumped */
	int dumpDatabase(Database db, WritableByteChannel out);
	/** @return the number of records dumped */
	int dumpTable(Database db, String tablename, WritableByteChannel out);

	/** @return the number of tables loaded */
	int loadDatabase(Database db, ReadableByteChannel in);
	/** @return the number of records loaded */
	int loadTable(Database db, String tablename, ReadableByteChannel in);

	enum Status { OK, CORRUPTED, UNRECOVERABLE };
	static interface Observer {
		void print(String msg);
	}
	Status check(String dbFilename, Observer ob);

	/** @return the number of tables copies */
	int compact(Database srcdb, Database dstdb);

	/** @return null if rebuild fails, otherwise result description */
	String rebuild(String dbFilename, String tempFilename);

	Record minRecord();
	Record maxRecord();

	Observer printObserver = new Observer() {
		@Override
		public void print(String msg) {
			System.out.print(msg);
		} };

	Observer nullObserver = new Observer() {
		@Override
		public void print(String msg) {
		} };

}
