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
	public void dumpDatabasePrint(String db_filename, String output_filename) {
		DbDump.dumpDatabasePrint(db_filename, output_filename);
	}

	@Override
	public int dumpDatabase(suneido.intfc.database.Database db, String output_filename) {
		return DbDump.dumpDatabase(db, output_filename);
	}

	@Override
	public void dumpTablePrint(String db_filename, String tablename) {
		DbDump.dumpTablePrint(db_filename, tablename);
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
	public void compactPrint(String db_filename) throws InterruptedException {
		DbCompact.compactPrint(db_filename);

	}

	@Override
	public void compact2(String db_filename, String tempfilename) {
		DbCompact.compact2(db_filename, tempfilename);
	}

	@Override
	public void rebuildOrExit(String db_filename) {
		DbRebuild.rebuildOrExit(db_filename);
	}

	@Override
	public void rebuild2(String db_filename, String tempfilename) {
		DbRebuild.rebuild2(db_filename, tempfilename);
	}

	@Override
	public void loadTablePrint(String tablename) {
		DbLoad.loadTablePrint(tablename);
	}

	@Override
	public void loadDatabasePrint(String filename, String db_filename) throws InterruptedException {
		DbLoad.loadDatabasePrint(filename, db_filename);
	}

	@Override
	public void load2(String filename, String db_filename) {
		DbLoad.load2(filename, db_filename);
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
