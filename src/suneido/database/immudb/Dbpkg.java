/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import suneido.util.FileUtils;

/**
 * A facade providing access to the database from the rest of jSuneido.
 * Just static methods, no instances.
 */
public class Dbpkg {
	public static final String DB_FILENAME = "suneido.db"; // will have .dbd and .dbi
	public static final Record MIN_RECORD = Record.EMPTY;
	public static final Record MAX_RECORD = new RecordBuilder().add(Record.MAX_FIELD).bufRec();

	private Dbpkg() {
	}

	public static Database create(String filename) {
		return Database.create(filename);
	}

	public static Database open(String filename) {
		return Database.open(filename);
	}

	public static Database openReadonly(String filename) {
		return Database.openReadonly(filename);
	}

	public static Database testdb() {
		return Database.create("", new HeapStorage(), new HeapStorage());
	}

	public static Record record(ByteBuffer buf) {
		return Record.from(buf);
	}

	public static Record record(int recadr, ByteBuffer buf) {
		return Record.from(recadr, buf);
	}

	public enum Status { OK, CORRUPTED, UNRECOVERABLE }

	public static Status check(String dbFilename, Observer ob) {
		return DbCheck.check(dbFilename, ob);
	}

	public static int dumpDatabase(Database db, WritableByteChannel out) {
		return DbDump.dumpDatabase(db, out);
	}

	public static int dumpTable(Database db, String tablename,
			WritableByteChannel out) {
		return DbDump.dumpTable(db, tablename, out);
	}

	public static int loadDatabase(Database db, ReadableByteChannel in) {
		return DbLoad.loadDatabase(db, in);
	}

	public static int loadTable(Database db, String tablename,
			ReadableByteChannel in) {
		return DbLoad.loadTable(db, tablename, in);
	}

	public static int compact(Database srcdb, Database dstdb) {
		return DbCompact.compact(srcdb, dstdb);
	}

	public static String rebuild(String dbFilename, String tempfilename) {
		return DbRebuild.rebuild(dbFilename, tempfilename);
	}

	public static String rebuildFromData(String dbFilename, String tempfilename) {
		return DbRebuild.rebuildFromData(dbFilename, tempfilename);
	}

	public static void renameDbWithBackup(String tempfile, String dbFilename) {
		FileUtils.renameWithBackup(tempfile + "d", dbFilename + "d");
		FileUtils.renameWithBackup(tempfile + "i", dbFilename + "i");
		if (new File(tempfile + "c").exists())
			FileUtils.renameWithBackup(tempfile + "c", dbFilename + "c");
	}

	public static RecordStore recordStore() {
		return new RecordStore();
	}

	public static void setOption(String name, Object value) {
		if (name.equals("max_update_tran_sec"))
			Transactions.MAX_UPDATE_TRAN_DURATION_SEC = (Integer) value;
		if (name.equals("max_writes_per_tran"))
			UpdateTransaction.MAX_WRITES_PER_TRANSACTION = (Integer) value;
	}

	public static boolean dbExists(String dbFilename) {
		return new File(dbFilename + "d").exists() &&
				new File(dbFilename + "i").exists();
	}

	interface Observer {
		void print(String msg);
	}

	public static Observer printObserver = System.out::print;

	public static Observer nullObserver = (String msg) -> { };

	public static class StringObserver implements Observer {
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
}
