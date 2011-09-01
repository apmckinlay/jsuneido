/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class DatabasePackage implements suneido.intfc.database.DatabasePackage {
	public static final DatabasePackage dbpkg = new DatabasePackage();
	static final String DB_FILENAME = "suneido.db";
	private static final Record MIN_RECORD = new Record();
	private static final Record MAX_RECORD = new Record().add(Record.MAX_FIELD);

	private DatabasePackage() {
	}

	@Override
	public Database create(String filename) {
		return new Database(filename, Mode.CREATE);
	}

	@Override
	public Database open(String filename) {
		return new Database(filename, Mode.OPEN);
	}

	@Override
	public Database openReadonly(String filename) {
		return new Database(filename, Mode.READ_ONLY);
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
	public int dumpDatabase(suneido.intfc.database.Database db, WritableByteChannel out) {
		return DbDump.dumpDatabase((Database) db, out);
	}

	@Override
	public int dumpTable(suneido.intfc.database.Database db, String tablename,
			WritableByteChannel out) {
		return DbDump.dumpTable((Database) db, tablename, out);
	}

	@Override
	public int loadDatabase(suneido.intfc.database.Database db, ReadableByteChannel in) {
		return DbLoad.loadDatabase((Database) db, in);
	}

	@Override
	public int loadTable(suneido.intfc.database.Database db, String tablename,
			ReadableByteChannel in) {
		return DbLoad.loadTable((Database) db, tablename, in);
	}

	@Override
	public void check(String dbFilename) {

	}

	@Override
	public void checkPrintExit(String filename) {
		DbCheck.checkPrintExit(filename);
	}

	@Override
	public void compact(String dbFilename, String tempfilename) {
		DbCompact.compact(dbFilename, tempfilename);
	}

	@Override
	public void compactPrint(String dbFilename) throws InterruptedException {
		DbCompact.compactPrint(dbFilename);

	}

	@Override
	public void rebuild(String dbFilename, String tempfilename) {
		DbRebuild.rebuild(dbFilename, tempfilename);
	}

	@Override
	public void rebuildOrExit(String dbFilename) {
		DbRebuild.rebuildOrExit(dbFilename);
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
