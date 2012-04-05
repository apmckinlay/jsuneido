/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;

import suneido.immudb.Bootstrap.TN;
import suneido.util.FileUtils;

import com.google.common.collect.Lists;

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
		List<Record> schemaRemoves = Lists.newArrayList();

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
System.out.println("remove " + r.tblnum + " - " + r);
			r.address = 0;
			if (type == 's') {
				schemaRemoves.add(r);
//				switch (r.tblnum) {
//				case TN.TABLES:
//					String tablename = r.getString(1);
//					db.dropTable(tablename);
//					break;
//				case TN.COLUMNS:
//					Column col = new Column(r);
//					tbEnsure(col.tblnum);
//					tb.dropColumn(col.name);
//					break;
//				case TN.INDEXES:
//					Index ix = new Index(r);
//					tbEnsure(ix.tblnum);
//					tb.dropIndex(ix.colNums);
//					break;
//				default:
//					assert false : "invalid schema table number " + r.tblnum;
//				}
			} else  if (type == 'u') {
				t.removeRecord(r.tblnum, r);
			}
			//TODO handle load/compact transactions
		}

		//TODO handle table numbers changing
		//TODO handle deleted columns

		@Override
		void add(Record r) {
System.out.println("add " + r.tblnum + " + " + r);
			if (skip)
				return;
			if (type == 's') {
				if (rename(r))
					return;
				handleSchemaRemoves();
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
				default:
					 //TODO handle load/compact transactions
					throw new UnsupportedOperationException();
				}
			} else if (type == 'u') {
				t.addRecord(r.tblnum, r);
			}
		}

		private boolean rename(Record r) {
			if (r.tblnum != TN.TABLES && r.tblnum != TN.COLUMNS)
				return false;
			if (schemaRemoves.size() != 1 ||
					schemaRemoves.get(0).tblnum != r.tblnum)
				return false;
			Record rr = schemaRemoves.get(0);
			int tblnum = r.getInt(0);
			if (rr.getInt(0) != tblnum)
				return false;
			if (r.tblnum == TN.TABLES) {
				String from = rr.getString(1);
				String to = r.getString(1);
System.out.println("renameTable " + from + " => " + to);
				db.renameTable(from, to);
			} else { // TN.COLUMNS
				String from = rr.getString(2);
				String to = r.getString(2);
				TableBuilder tb = db.alterTable(tableName(tblnum));
System.out.println("renameColumn " + from + " => " + to);
				tb.renameColumn(from, to);
				tb.finish();
			}
			schemaRemoves.clear();
			return true;
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
			else if (t != null)
				t.ck_complete();
			else
				handleSchemaRemoves();
		}

		private void handleSchemaRemoves() {
			if (schemaRemoves.isEmpty())
				return;
			handleSchemaRemoves2();
			schemaRemoves.clear();
		}

		private void handleSchemaRemoves2() {
			for (Record r : schemaRemoves)
				if (r.tblnum == TN.TABLES) {
					String tablename = r.getString(1);
System.out.println("dropTable " + tablename);
					db.dropTable(tablename);
					return;
				}
			for (Record r : schemaRemoves)
				if (r.tblnum == TN.INDEXES) {
					Index ix = new Index(r);
					tbEnsure(ix.tblnum);
System.out.println("dropIndex " + ix);
					tb.dropIndex(ix.colNums);
				}
			for (Record r : schemaRemoves)
				if (r.tblnum == TN.COLUMNS) {
					Column col = new Column(r);
					tbEnsure(col.tblnum);
System.out.println("dropColumn " + col);
					tb.dropColumn(col.name);
				}
			tb.finish();
			tb = null;
		}

	}

	public static void main(String[] args) {
		rebuild("immu.db", "immu.rbld");
	}

}
