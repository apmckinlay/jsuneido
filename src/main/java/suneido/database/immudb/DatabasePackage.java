/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import suneido.util.FileUtils;

/** singleton */
public class DatabasePackage implements suneido.intfc.database.DatabasePackage {
	public static final DatabasePackage dbpkg = new DatabasePackage();
	static final String DB_FILENAME = "suneido.db"; // will have .dbd and .dbi
	static final Record MIN_RECORD = Record.EMPTY;
	static final Record MAX_RECORD = new RecordBuilder().add(Record.MAX_FIELD).bufRec();

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
		return Database.create("", new HeapStorage(), new HeapStorage());
	}

	@Override
	public RecordBuilder recordBuilder() {
		return new RecordBuilder();
	}

	@Override
	public Record record(ByteBuffer buf) {
		return Record.from(buf);
	}

	@Override
	public Record record(int recadr, ByteBuffer buf) {
		return Record.from(recadr, buf);
	}

	@Override
	public Status check(String dbFilename, Observer ob) {
		return DbCheck.check(dbFilename, ob);
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
	public int compact(suneido.intfc.database.Database srcdb,
			suneido.intfc.database.Database dstdb) {
		return DbCompact.compact((Database) srcdb, (Database) dstdb);
	}

	@Override
	public String rebuild(String dbFilename, String tempfilename) {
		return DbRebuild.rebuild(dbFilename, tempfilename);
	}

	@Override
	public String rebuildFromData(String dbFilename, String tempfilename) {
		return DbRebuild.rebuildFromData(dbFilename, tempfilename);
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
		return "(immudb)";
	}

	@Override
	public void renameDbWithBackup(String tempfile, String dbFilename) {
		FileUtils.renameWithBackup(tempfile + "d", dbFilename + "d");
		FileUtils.renameWithBackup(tempfile + "i", dbFilename + "i");
		if (new File(tempfile + "c").exists())
			FileUtils.renameWithBackup(tempfile + "c", dbFilename + "c");
	}

	@Override
	public RecordStore recordStore() {
		return new RecordStore();
	}

	@Override
	public void setOption(String name, Object value) {
		if (name.equals("max_update_tran_sec"))
			Transactions.MAX_UPDATE_TRAN_DURATION_SEC = (Integer) value;
		if (name.equals("max_writes_per_tran"))
			UpdateTransaction.MAX_WRITES_PER_TRANSACTION = (Integer) value;
	}

	@Override
	public boolean dbExists(String dbFilename) {
		return new File(dbFilename + "d").exists() &&
				new File(dbFilename + "i").exists();
	}

}