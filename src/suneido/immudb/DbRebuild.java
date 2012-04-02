/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;

import suneido.util.FileUtils;

public class DbRebuild {
	private final String dbFilename;
	private final String tempFilename;
	private final Storage dstor;
	private final Storage istor;
	private final Check check;

	/** @return A completion string if successful, null if not. */
	public static String rebuild(String dbFilename, String tempFilename) {
		return new DbRebuild(dbFilename, tempFilename).rebuild();
	}

	private DbRebuild(String dbFilename, String tempFilename) {
		this.dbFilename = dbFilename;
		this.tempFilename = tempFilename;
		dstor = new MmapFile(dbFilename + "d", "r");
		istor = //new MmapFile(dbFilename + "i", "r");
new MemStorage();
Dump.data(dstor);
		check = new Check(dstor, istor);
	}

	private String rebuild() {
		try {
			check.fullcheck();
			fix();
			return "Last commit "; //TODO use last good data date
					// + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(check.lastOkDate());
		} /*catch (Exception e) {
			return null;
		} */ finally {
			dstor.close();
			istor.close();
		}
	}

	private void fix() {
		copyGoodPrefix();
		reprocess();
	}

	private void copyGoodPrefix() {
		if (check.dIter.okSize == 0)
			return;
		try {
			FileUtils.copy(new File(dbFilename + "d"), new File(tempFilename + "d"),
					check.dIter.okSize);
			FileUtils.copy(new File(dbFilename + "i"), new File(tempFilename + "i"),
					check.iIter.okSize);
		} catch (IOException e) {
			throw new RuntimeException("Rebuild copy failed", e);
		}
	}

	/** reprocess any good data after last matching persist */
	// could copy remaining good data and then process in place
	// but to start it's simpler to not copy and to apply normally to new db
	private void reprocess() {
		Database db = check.dIter.okSize == 0
				? Database.create(tempFilename)
				: Database.open(tempFilename);
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
		UpdateTransaction t;

		Proc(Database db, Storage stor, int adrFrom) {
			super(stor, adrFrom);
			this.db = db;
		}

		@Override
		void type(char c) {
			type = c;
			skip = (commitAdr == Storage.FIRST_ADR); // skip bootstrap commit
			if (type == 'u')
				t = db.updateTransaction();
		}

		@Override
		void remove(Record r) {
			r.address = 0;
			t.removeRecord(r.tblnum, r);
		}

		//TODO handle table numbers changing
		//TODO handle deleted columns

		@Override
		void add(Record r) {
			if (skip)
				return;
			if (type == 'e') {
				// assume schema tran
				if (r.tblnum == 1) {
					String tablename = r.getString(1);
					tb = db.createTable(tablename);
					tblnames.put(r.getInt(0), tablename);
				} else if (r.tblnum == 2) {
					Column col = new Column(r);
					tbEnsure(col.tblnum);
					tb.addColumn(col.name);
				} else if (r.tblnum == 3) {
					Index ix = new Index(r);
					tbEnsure(ix.tblnum);
					tb.addIndex(ix.colNums, ix.isKey, ix.unique,
							ix.fksrc == null ? "" : ix.fksrc.tablename,
							ix.fksrc == null ? "" : ix.fksrc.columns,
							ix.fksrc == null ? 0 : ix.fksrc.mode);
				}
			} else if (type == 'u') {
				t.addRecord(r.tblnum, r);
			}
		}

		private void tbEnsure(int tblnum) {
			if (tb != null)
				return;
			String tableName = tblnames.get(tblnum);
			assert tableName != null : "unknown tblnum " + tblnum;
			tb = db.alterTable(tableName);
		}

		@Override
		void after() {
			if (tb != null)
				tb.finish();
			else if (t != null)
				t.ck_complete();
		}

	}

	public static void main(String[] args) {
		rebuild("immu.db", "immu.rbld");
	}

}
