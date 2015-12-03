/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Util.capitalize;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import gnu.trove.map.hash.TIntObjectHashMap;
import suneido.immudb.Bootstrap.TN;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.util.Errlog;
import suneido.util.FileUtils;

class DbRebuild {
	private final String oldFilename;
	private final String newFilename;
	private final Storage dstor;
	private Storage istor;

	/** @return A completion string if successful, null if not. */
	public static String rebuild(String oldFilename, String newFilename) {
		return new DbRebuild(oldFilename, newFilename, false).rebuild();
	}

	public static String rebuildFromData(String oldFilename, String newFilename) {
		return new DbRebuild(oldFilename, newFilename, true).rebuildFromData();
	}

	private DbRebuild(String oldFilename, String newFilename, boolean fromData) {
		this.oldFilename = oldFilename;
		this.newFilename = newFilename;
		dstor = new MmapFile(oldFilename + "d", "r");
		istor = fromData ? null : new MmapFile(oldFilename + "i", "r");
	}

	// for tests
	DbRebuild(Storage dstor, Storage istor) {
		this.oldFilename = "";
		this.newFilename = "";
		this.dstor = dstor;
		this.istor = istor;
	}

	/** @return null on failure, else success message */
	protected String rebuild() {
		try {
			System.out.println("Checking...");
			Check check = new Check(dstor, istor);
			System.out.println("checksums...");
			if (check.fullcheck()) {
				if (check_data_and_indexes(dstor, istor)) {
					new File(newFilename).delete();
					System.out.println("OK Last good commit " +
							format(check.lastOkDate()));
					return "database appears OK, no rebuild done";
				} // else indexes corrupt
			} else if (check.lastOkDate() != null) { // checksum error
				System.out.println("Data and indexes match up to " +
							format(check.lastOkDate()));
					String result = fix(check);
					if (result != null)
						return result;
				// else fall through
			}
			System.out.println("No usable indexes");
			// NOTE: at this point it could be bad data rather than bad index
			// in which case rebuilding from data won't help.
			// Could avoid this with better info from check_data_and_indexes.
			return null;
		} catch (Throwable e) {
			Errlog.error("Rebuild", e);
			return null;
		} finally {
			dstor.close();
			if (istor != null)
				istor.close();
		}
	}

	private static boolean check_data_and_indexes(Storage dstor, Storage istor) {
		System.out.println("tables...");
		DbCheck dbCheck = new DbCheck("", dstor, istor, DatabasePackage.printObserver);
		boolean ok = dbCheck.check_data_and_indexes();
		System.out.print(dbCheck.details);
		return ok;
	}

	private static String format(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
	}

	/** Ignore any indexes (dbi) and rebuild just from data (dbd)
	 *  @return null on failure, else success message */
	protected String rebuildFromData() {
		System.out.println("Rebuild from data ...");
		try {
			assert istor == null;
			istor = new HeapStorage(); // ignore existing indexes
			Check check = new Check(dstor, istor);
			check.fullcheck();
			return fix(check);
		} catch (Throwable e) {
			Errlog.error("Rebuild from data", e);
			return null;
		} finally {
			dstor.close();
		}
	}

	/** @return null on failure, else success message */
	protected String fix(Check check) {
		copyGoodPrefix(check);
		try(Database db = newdb(check.dOkSize())) {
			assert db != null;
			if (check.dIterNotFinished())
				System.out.println("Reprocessing data ...");
			Date lastOkDate = reprocess(db, check);
			System.out.println("Checking rebuilt database ...");
			db.persist();
			Status status = DbCheck.check(newFilename, db.dstor, db.istor,
					DatabasePackage.printObserver);
			if (status != Status.OK) {
				System.out.println("Check after rebuild FAILED");
				return null;
			}
			return (lastOkDate == null)
				? null
				: "Last good commit " + format(lastOkDate);
		}
	}

	// overridden by tests
	protected Database newdb(long dOkSize) {
		return (dOkSize == 0)
				? Database.create(newFilename)
				: Database.openWithoutCheck(newFilename);
	}

	private void copyGoodPrefix(Check check) {
		if (check.dOkSize() == 0)
			return;
		try {
			System.out.println("Copying " + fmt(check.dOkSize()) + " bytes of data file...");
			FileUtils.copy(new File(oldFilename + "d"), new File(newFilename + "d"),
					check.dOkSize());
			long discard = istor.sizeFrom(0) - check.iOkSize();
			System.out.println("Copying " + fmt(check.iOkSize()) + " bytes of index file" +
					" (discarding " + fmt(discard) + ")...");
			FileUtils.copy(new File(oldFilename + "i"), new File(newFilename + "i"),
					check.iOkSize());
		} catch (IOException e) {
			throw new RuntimeException("Rebuild copy failed", e);
		}
	}

	String fmt(long n) {
		return String.format("%,d", n);
	}

	/** reprocess any good data after last matching persist */
	// could copy remaining good data and then process in place
	// but simpler to not copy and to apply normally to new db
	Date reprocess(Database db, Check check) {
		Date lastOkDate = check.lastOkDate();
		long lastOkSize = check.dOkSize();
		StorageIter dIter = new StorageIter(dstor, Storage.offsetToAdr(lastOkSize));
		while (dIter.notFinished()) {
			try {
				new Proc(db, check.dOkSize(), dstor, dIter.adr()).process();
			} catch(Throwable e) {
				System.err.println("offset: " + Storage.adrToOffset(dIter.adr()));
				System.err.println(e);
				throw e;
			}
			lastOkDate = dIter.date();
			lastOkSize = dIter.sizeInc();
			dIter.advance();
		}
		long discard = dstor.sizeFrom(0) - lastOkSize;
		if (discard == 0)
			System.out.println("Recovered all data");
		else
			System.out.println("Could not recover " + fmt(discard) + " bytes of data");
		return lastOkDate;
	}

	private final TIntObjectHashMap<String> tblnames = new TIntObjectHashMap<>();

	private class Proc extends CommitProcessor {
		private final Database db;
		private final long copiedDataSize;
		private final ReadTransaction rt;
		char type;
		boolean skip;
		TableBuilder tb;
		UpdateTransaction ut;
		BulkTransaction bt;
		int bulkTblnum = 0;
		int first = 0;
		int last = 0;

		Proc(Database db, long copiedDataSize, Storage stor, int adrFrom) {
			super(stor, adrFrom);
			this.db = db;
			this.copiedDataSize = copiedDataSize;
			this.rt = db.readTransaction();
		}

		@Override
		void type(char c) {
			type = c;
			skip = (commitAdr == stor.FIRST_ADR); // skip bootstrap commit
			if (type == 'u')
				ut = new RebuildTransaction(db);
			else if (type == 'b')
				bt = db.bulkTransaction();
		}

		@Override
		void add(DataRecord r) {
			if (skip)
				return;
			r.address(0);
			if (type == 'u') {
				ut.addRecord(r.tblnum(), r);
			} else if (type == 's') {
				switch (r.tblnum()) {
				case TN.TABLES:
					String tablename = r.getString(1);
					tb = db.createTable(tablename);
					int tblnum = r.getInt(0);
					assert tb.tblnum() == tblnum;
					tblnames.put(tblnum, tablename);
					break;
				case TN.COLUMNS:
					Column col = new Column(r);
					tbEnsure(col.tblnum);
					tb.addColumn(col.field >= 0 ? col.name : capitalize(col.name));
					break;
				case TN.INDEXES:
					Index ix = new Index(r);
					tbEnsure(ix.tblnum);
					tb.addIndex(ix.colNums, ix.isKey, ix.unique,
							ix.fksrc == null ? "" : ix.fksrc.tablename,
							ix.fksrc == null ? "" : ix.fksrc.columns,
							ix.fksrc == null ? 0 : ix.fksrc.mode);
					break;
				case TN.VIEWS:
					db.addView(r.getString(0), r.getString(1));
					break;
				default:
					assert false : "invalid schema table number " + r.tblnum();
				}
			} else if (type == 'b') {
				if (bulkTblnum == 0)
					bulkTblnum = r.tblnum();
				assert r.tblnum() == bulkTblnum;
				last = bt.loadRecord(r.tblnum(), r);
				if (first == 0)
					first = last;
			} else
				assert false : "invalid type " + type;
		}

		@Override
		void remove(DataRecord r) {
			clearAddress(r);
			if (type == 'u') {
				ut.removeRecord(r.tblnum(), r);
			} else if (type == 's') {
				switch (r.tblnum()) {
				case TN.TABLES:
					String tablename = r.getString(1);
					db.dropTable(tablename);
					int tblnum = r.getInt(0);
					tblnames.remove(tblnum);
					break;
				case TN.COLUMNS:
					Column col = new Column(r);
					if (tblnames.contains(col.tblnum)) {
						tbEnsure(col.tblnum);
						tb.dropColumn(col.name);
					}
					break;
				case TN.INDEXES:
					Index ix = new Index(r);
					if (tblnames.contains(ix.tblnum)) {
						tbEnsure(ix.tblnum);
						tb.dropIndex(ix.colNums);
					}
					break;
				case TN.VIEWS:
					db.dropTable(r.getString(0));
					break;
				default:
					assert false : "invalid schema table number " + r.tblnum();
				}
			} else
				assert false : "invalid type " + type;
		}

		@Override
		void update(DataRecord from, DataRecord to) {
			clearAddress(from);
			to.address(0);
			assert from.tblnum() == to.tblnum() : "from " + from.tblnum() + " to " + to.tblnum();
			if (type == 'u') {
				assert from.tblnum() == to.tblnum();
				ut.updateRecord(from.tblnum(), from, to);
			} else if (type == 's') {
				if (from.tblnum() == TN.TABLES) {
					String before = from.getString(1);
					String after = to.getString(1);
					db.renameTable(before, after);
				} else if (from.tblnum() == TN.COLUMNS) {
					String before = from.getString(2);
					String after = to.getString(2);
					int tblnum = from.getInt(0);
					db.alterTable(tableName(tblnum))
							.renameColumn(before, after)
							.finish();
				}
			} else
				assert false : "invalid type " + type;
		}

		void clearAddress(DataRecord r) {
			if (Storage.adrToOffset(r.address()) >= copiedDataSize)
				r.address(0); // may have changed
		}

		private void tbEnsure(int tblnum) {
			if (tb != null)
				return;
			tb = db.alterTable(tableName(tblnum));
		}

		protected String tableName(int tblnum) {
			String tableName = tblnames.get(tblnum);
			if (tableName == null)
				tableName = rt.getTable(tblnum).name;
			assert tableName != null : "unknown tblnum " + tblnum;
			return tableName;
		}

		@Override
		void after() {
			if (tb != null)
				tb.finish();
			else if (ut != null)
				ut.ck_complete();
			else if (bt != null) {
				DbLoad.createIndexes(bt, bt.getTable(bulkTblnum), first, last);
				bt.ck_complete();
			}
		}

	} // end of Proc

	/** no foreign keys or triggers */
	static class RebuildTransaction extends UpdateTransaction {
		RebuildTransaction(Database db) {
			super(db.trans.nextNum(false), db);
		}
		@Override
		protected void indexedDataIndex(IndexedData id, Table table,
				Index index, TranIndex btree, String colNames) {
			id.index(btree, index.mode(), index.colNums, colNames);
		}
		@Override
		public void callTrigger(suneido.intfc.database.Table table,
				suneido.intfc.database.Record oldrec,
				suneido.intfc.database.Record newrec) {
		}
	}

//	public static void main(String[] args) {
//		String dbname = "suneido.db";
//		String result = rebuild(dbname, dbname + "rb");
//		if (result == null)
//			System.out.println("Rebuild " + dbname + ": FAILED");
//		else
//			System.out.println("Rebuild " + dbname + ": " + result);
//	}

}
