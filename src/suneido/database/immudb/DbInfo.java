/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.Translator;

public class DbInfo {
	private final Tran tran;
	private DbHashTrie dbinfo;

	/** used by Bootstrap */
	public DbInfo(Tran tran, TableInfo... info) {
		this.tran = tran;
		dbinfo = DbHashTrie.empty(tran.stor);
		for (TableInfo ti : info)
			dbinfo = dbinfo.with(ti);
	}

	public DbInfo(Tran tran, int adr) {
		this.tran = tran;
		dbinfo = DbHashTrie.from(tran.stor, adr);
	}

	public TableInfo get(int tblnum) {
		Entry e = dbinfo.get(tblnum);
		if (e instanceof IntEntry) {
			int adr = ((IntEntry) e).value;
			Record rec = tran.getrec(adr);
			TableInfo ti = new TableInfo(rec);
			dbinfo = dbinfo.with(ti);
			return ti;
		} else
			return (TableInfo) e;
	}

	public void add(TableInfo ti) {
		dbinfo = dbinfo.with(ti);
	}

	public int store() {
		return dbinfo.store(new DbInfoTranslator());
	}

	private class DbInfoTranslator implements Translator {
		@Override
		public int translate(Entry e) {
			if (e instanceof TableInfo) {
				Record r = ((TableInfo) e).toRecord();
				return r.store(tran.stor);
			} else
				return ((IntEntry) e).value;
		}
	}

}
