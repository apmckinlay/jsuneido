/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;

import suneido.immudb.Bootstrap.TN;
import suneido.util.FileUtils;

class DbRebuild {
	private final String oldFilename;
	private final String newFilename;
	private final Storage dstor;
	private final Storage istor;
	private Check check;

	/** @return A completion string if successful, null if not. */
	public static String rebuild(String oldFilename, String newFilename) {
		return new DbRebuild(oldFilename, newFilename).rebuild();
	}

	private DbRebuild(String oldFilename, String newFilename) {
		this.oldFilename = oldFilename;
		this.newFilename = newFilename;
		dstor = new MmapFile(oldFilename + "d", "r");
		if (new File(oldFilename + "i").canRead())
			istor = new MmapFile(oldFilename + "i", "r");
		else
			istor = new MemStorage();
	}

	// for tests
	DbRebuild(Storage dstor, Storage istor) {
		this.oldFilename = "infile";
		this.newFilename = "outfile";
		this.dstor = dstor;
		this.istor = istor;
	}

	protected String rebuild() {
		check = new Check(dstor, istor);
		try {
			check.fullcheck();
			fix();
			return "Last commit "; //TODO
					//+ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(check.lastOkDate());
		} catch (Exception e) {
			return null;
		} finally {
//System.out.println("AFTER ======================");
//Dump.data(dstor);
			dstor.close();
			istor.close();
		}
	}

	/** overridden by tests */
	protected void fix() {
		copyGoodPrefix();
		Database db = (check.dIter.okSize == 0)
				? Database.create(newFilename)
				: Database.open(newFilename);
		try {
			reprocess(db);
		} finally {
			db.close();
		}
	}

	private void copyGoodPrefix() {
		if (check.dIter.okSize == 0)
			return;
		try {
			FileUtils.copy(new File(oldFilename + "d"), new File(newFilename + "d"),
					check.dIter.okSize);
			FileUtils.copy(new File(oldFilename + "i"), new File(newFilename + "i"),
					check.iIter.okSize);
		} catch (IOException e) {
			throw new RuntimeException("Rebuild copy failed", e);
		}
	}

	/** reprocess any good data after last matching persist */
	// could copy remaining good data and then process in place
	// but to start it's simpler to not copy and to apply normally to new db
	void reprocess(Database db) {
		while (check.dIter.hasNext()) {
			new Proc(db, dstor, check.dIter.adr).process();
			check.dIter.advance();
		}
		db.close();
	}

	private final TIntObjectHashMap<String> tblnames = new TIntObjectHashMap<String>();

	private class Proc extends CommitProcessor {
		private final Database db;
		char type;
		boolean skip;
		TableBuilder tb;
		UpdateTransaction ut;
		BulkTransaction bt;
		int bulkTblnum = 0;
		int first = 0;
		int last = 0;

		Proc(Database db, Storage stor, int adrFrom) {
			super(stor, adrFrom);
			this.db = db;
		}

		@Override
		void type(char c) {
			type = c;
			skip = (commitAdr == Storage.FIRST_ADR); // skip bootstrap commit
			if (type == 'u')
				ut = db.updateTransaction();
			else if (type == 'b')
				bt = db.bulkTransaction();
		}

		@Override
		void add(Record r) {
			if (skip)
				return;
			r.address = 0;
			if (type == 'u') {
				ut.addRecord(r.tblnum, r);
			} else if (type == 's') {
				switch (r.tblnum) {
				case TN.TABLES:
					String tablename = r.getString(1);
					tb = db.createTable(tablename);
					tblnames.put(r.getInt(0), tablename);
					break;
				case TN.COLUMNS:
					Column col = new Column(r);
					tbEnsure(col.tblnum);
					tb.addColumn(col.name);
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
					assert false : "invalid schema table number " + r.tblnum;
				}
			} else if (type == 'b') {
				if (bulkTblnum == 0)
					bulkTblnum = r.tblnum;
				assert r.tblnum == bulkTblnum;
				last = bt.loadRecord(r.tblnum, r);
				if (first == 0)
					first = last;
			} else
				assert false : "invalid type " + type;
		}

		@Override
		void remove(Record r) {
			r.address = 0;
			if (type == 'u') {
				ut.removeRecord(r.tblnum, r);
			} else if (type == 's') {
				switch (r.tblnum) {
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
					assert false : "invalid schema table number " + r.tblnum;
				}
			} else
				assert false : "invalid type " + type;
		}

		@Override
		void update(Record from, Record to) {
			assert from.tblnum == to.tblnum;
			if (type == 'u') {
				ut.updateRecord(from.tblnum, from, to);
			} else if (type == 's') {
				if (from.tblnum == TN.TABLES) {
					String before = from.getString(1);
					String after = to.getString(1);
					db.renameTable(before, after);
				} else if (from.tblnum == TN.COLUMNS) {
					String before = from.getString(2);
					String after = to.getString(2);
					int tblnum = from.getInt(0);
					TableBuilder tb = db.alterTable(tableName(tblnum));
					tb.renameColumn(before, after);
					tb.finish();
				}
			} else
				assert false : "invalid type " + type;
		}

		private void tbEnsure(int tblnum) {
			if (tb != null)
				return;
			tb = db.alterTable(tableName(tblnum));
		}

		protected String tableName(int tblnum) {
			String tableName = tblnames.get(tblnum);
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

	public static void main(String[] args) {
		rebuild("immu.db", "immu.rbld");
	}

}
