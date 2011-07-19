/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;

public class DatabasePackage implements suneido.intfc.database.DatabasePackage {

	@Override
	public Database open(String filename) {
		return new Database(filename, Mode.OPEN);
	}

	@Override
	public int offsetToInt(long offset) {
		return Mmfile.offsetToInt(offset);
	}

	@Override
	public long intToOffset(int i) {
		return Mmfile.intToOffset(i);
	}

	@Override
	public Database testdb() {
		return new Database(new DestMem(), Mode.CREATE);
	}

	@Override
	public Record record() {
		return new Record();
	}

	@Override
	public Record record(int size) {
		return new Record(size);
	}

	@Override
	public Record record(ByteBuffer buf) {
		return new Record(buf);
	}

	@Override
	public Record minRecord() {
		return Record.MINREC;
	}

	@Override
	public Record maxRecord() {
		return Record.MAXREC;
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

}
