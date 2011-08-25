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
	public void loadTablePrint(String tablename) {
		DbLoad.loadTablePrint(tablename, DB_FILENAME);
	}

	@Override
	public void dumpDatabasePrint(String db_filename, String output_filename) {
		// TODO dumpDatabasePrint
	}

	@Override
	public int dumpDatabase(suneido.intfc.database.Database db,
			String output_filename) {
		// TODO dumpDatabase
		return 0;
	}

	@Override
	public void dumpTablePrint(String db_filename, String tablename) {
		// TODO dumpTablePrint
	}

	@Override
	public int dumpTable(suneido.intfc.database.Database db, String tablename) {
		// TODO dumpTable
		return 0;
	}

	@Override
	public void compactPrint(String db_filename) throws InterruptedException {
		// TODO compactPrint
	}

	@Override
	public void compact2(String db_filename, String tempfilename) {
		// TODO compact2
	}

	@Override
	public void rebuildOrExit(String db_filename) {
		// TODO rebuildOrExit
	}

	@Override
	public void rebuild2(String db_filename, String tempfilename) {
		// TODO rebuild2
	}

	@Override
	public void loadDatabasePrint(String string, String string2) throws InterruptedException {
		// TODO loadDatabasePrint
	}

	@Override
	public void load2(String string, String actionArg) {
		// TODO load2
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
