/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;

public class DatabasePackage implements suneido.intfc.database.DatabasePackage {
	static final String DB_FILENAME = "suneido.db";
	private static final Record MIN_RECORD = new Record();
	private static final Record MAX_RECORD = new Record().add(Record.MAX_FIELD);

	@Override
	public Database open(String filename) {
		return new Database(filename, Mode.OPEN);
	}

	@Override
	public Database testdb() {
		return new Database(new DestMem(), Mode.CREATE);
	}

	@Override
	public Record recordBuilder() {
		return new Record();
	}

	@Override
	public Record record(ByteBuffer buf) {
		return new Record(buf);
	}

	@Override
	public Record record(int recadr, ByteBuffer buf) {
		return new Record(Mmfile.intToOffset(recadr), buf);
	}

	@Override
	public void dumpDatabasePrint(String dbFilename, String outputFilename) {
		DbDump.dumpDatabasePrint(dbFilename, outputFilename);
	}

	@Override
	public int dumpDatabase(suneido.intfc.database.Database db, String outputFilename) {
		return DbDump.dumpDatabase(db, outputFilename);
	}

	@Override
	public void dumpTablePrint(String dbFilename, String tablename) {
		DbDump.dumpTablePrint(dbFilename, tablename);
	}

	@Override
	public int dumpTable(suneido.intfc.database.Database db, String tablename) {
		return DbDump.dumpTable(db, tablename);
	}

	@Override
	public void checkPrintExit(String filename) {
		DbCheck.checkPrintExit(filename);
	}

	@Override
	public void compactPrint(String dbFilename) throws InterruptedException {
		DbCompact.compactPrint(dbFilename);

	}

	@Override
	public void rebuildOrExit(String dbFilename) {
		DbRebuild.rebuildOrExit(dbFilename);
	}

	@Override
	public void loadDatabasePrint(String filename, String dbFilename) throws InterruptedException {
		DbLoad.loadDatabasePrint(filename, dbFilename);
	}

	@Override
	public void loadTablePrint(String tablename) {
		DbLoad.loadTablePrint(tablename);
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
