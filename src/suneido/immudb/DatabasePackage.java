/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

public class DatabasePackage implements suneido.intfc.database.DatabasePackage {
	static final String DB_FILENAME = "immu.db";
	static final Record MIN_RECORD = new RecordBuilder().build();
	static final Record MAX_RECORD = new RecordBuilder().add(Record.MAX_FIELD).build();

	@Override
	public Database open(String filename) {
		return Database.open(filename, "rw");
	}

	@Override
	public Database testdb() {
		return Database.create(new TestStorage(1024, 1024));
	}

	@Override
	public RecordBuilder recordBuilder() {
		return new RecordBuilder();
	}

	@Override
	public Record record(ByteBuffer buf) {
		return new Record(buf);
	}

	@Override
	public Record record(int recadr, ByteBuffer buf) {
		return new Record(recadr, buf);
	}

	@Override
	public void checkPrintExit(String filename) {
		DbCheck.checkPrintExit(filename);
	}

	@Override
	public void dumpDatabasePrint(String dbFilename, String outputFilename) {
		DbDump.dumpDatabasePrint(DB_FILENAME, "database.su");
	}

	@Override
	public int dumpDatabase(suneido.intfc.database.Database db,
			String outputFilename) {
		return DbDump.dumpDatabase(DB_FILENAME, "database.su");
	}

	@Override
	public void dumpTablePrint(String dbFilename, String tablename) {
		DbDump.dumpTablePrint(DB_FILENAME, tablename);
	}

	@Override
	public int dumpTable(suneido.intfc.database.Database db, String tablename) {
		return DbDump.dumpTable((Database) db, tablename);
	}

	@Override
	public void compactPrint(String dbFilename) throws InterruptedException {
		// TODO compactPrint
	}

	@Override
	public void compact2(String dbFilename, String tempfilename) {
		// TODO compact2
	}

	@Override
	public void rebuildOrExit(String dbFilename) {
		// TODO rebuildOrExit
	}

	@Override
	public void rebuild2(String dbFilename, String tempfilename) {
		// TODO rebuild2
	}

	@Override
	public void loadDatabasePrint(String filename, String dbFilename) throws InterruptedException {
		DbLoad.loadDatabasePrint(filename, dbFilename);
	}

	@Override
	public void loadTablePrint(String tablename) {
		DbLoad.loadTablePrint(tablename, DB_FILENAME);
	}

	@Override
	public Record minRecord() {
		return MIN_RECORD;
	}

	@Override
	public Record maxRecord() {
		return MAX_RECORD;
	}

	@Override
	public String dbFilename() {
		return DB_FILENAME;
	}

}
