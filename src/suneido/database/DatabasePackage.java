/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import suneido.util.FileUtils;

public class DatabasePackage implements suneido.intfc.database.DatabasePackage {
	public static final DatabasePackage dbpkg = new DatabasePackage();
	static final String DB_FILENAME = "suneido.db";
	private static final Record MIN_RECORD = new Record();
	private static final Record MAX_RECORD = new Record().add(Record.MAX_FIELD);

	private DatabasePackage() {
	}

	@Override
	public Database create(String filename) {
		return Database.create(filename);
	}

	@Override
	public Database open(String filename) {
		return Database.open(filename);
	}

	@Override
	public Database openReadonly(String filename) {
		return Database.openReadonly(filename);
	}

	@Override
	public Database testdb() {
		return Database.testdb();
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
	public Record recordCopy(ByteBuffer buf) {
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
	public Status check(String dbFilename, Observer ob) {
		return DbCheck.check(dbFilename, ob);
	}

	@Override
	public int compact(suneido.intfc.database.Database srcdb,
			suneido.intfc.database.Database dstdb) {
		return DbCompact.compact((Database) srcdb, (Database) dstdb);
	}

	@Override
	public String rebuild(String dbFilename, String tempfilename) {
		return DbRebuild.rebuild(dbFilename, tempfilename);
	}

	@Override
	public String forceRebuild(String dbFilename, String tempfilename) {
		// nothing extra required to force rebuild
		return rebuild(dbFilename, tempfilename);
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

	@Override
	public String name() {
		return "";
	}

	@Override
	public void renameDbWithBackup(String tempfile, String dbFilename) {
		FileUtils.renameWithBackup(tempfile, dbFilename);
	}

}
