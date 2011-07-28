/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

import suneido.util.PersistentMap;

@Immutable
class Tables {
	private final PersistentMap<Integer, Table> bynum;
	private final PersistentMap<String, Table> byname;
//	final PersistentMap<Index, ImmutableList<ForeignKey>> fkdsts;
	final int maxTblNum;

	Tables() {
		bynum = PersistentMap.empty();
		byname = PersistentMap.empty();
		maxTblNum = 0;
	}

	private Tables(PersistentMap<Integer, Table> bynum,
			PersistentMap<String, Table> byname, int maxTblNum) {
		this.bynum = bynum;
		this.byname = byname;
		this.maxTblNum = maxTblNum;
	}

	Table get(int tblnum) {
		return bynum.get(tblnum);
	}

	Table get(String tableName) {
		return byname.get(tableName);
	}

	Tables with(Table tbl) {
		return new Tables(bynum.with(tbl.num, tbl), byname.with(tbl.name, tbl),
				Math.max(tbl.num, maxTblNum));
	}

	Tables without(Table tbl) {
		// look up old name to handle rename
		Table old = bynum.get(tbl.num);
		if (old == null)
			return this;
		return new Tables(bynum.without(tbl.num), byname.without(old.name), maxTblNum);
	}

	static class Builder {
		private final PersistentMap.Builder<Integer, Table> bynum =
				PersistentMap.builder();
		private final PersistentMap.Builder<String, Table> byname =
				PersistentMap.builder();
		int maxTblNum = 0;

		void add(Table tbl) {
			bynum.put(tbl.num, tbl);
			byname.put(tbl.name, tbl);
			if (tbl.num > maxTblNum)
				maxTblNum = tbl.num;
		}

		Tables build() {
			return new Tables(bynum.build(), byname.build(), maxTblNum);
		}
	}

}
