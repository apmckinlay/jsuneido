/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import javax.annotation.concurrent.Immutable;

import suneido.util.PersistentMap;

/**
 * Stores table information for {@link Database}.
 * {@link Transaction}'s are given the current state when starting.
 * Immutable persistent so threadsafe.
 */
@Immutable
class Tables {
	private final PersistentMap<Integer, Table> bynum;
	private final PersistentMap<String, Table> byname;

	Tables() {
		this.bynum = PersistentMap.empty();
		this.byname = PersistentMap.empty();
	}

	private Tables(PersistentMap<Integer, Table> bynum,
			PersistentMap<String, Table> byname) {
		this.bynum = bynum;
		this.byname = byname;
	}

	Table get(int tblnum) {
		return bynum.get(tblnum);
	}

	Table get(String tblname) {
		return byname.get(tblname);
	}

	Tables with(Table tbl) {
		return new Tables(bynum.with(tbl.num, tbl), byname.with(tbl.name, tbl));
	}

	Tables without(Table tbl) {
		// look up old name to handle rename
		Table old = bynum.get(tbl.num);
		if (old == null)
			return this;
		return new Tables(bynum.without(tbl.num), byname.without(old.name));
	}

	static class Builder {
		private final PersistentMap.Builder<Integer, Table> bynum =
				PersistentMap.builder();
		private final PersistentMap.Builder<String, Table> byname =
				PersistentMap.builder();

		void add(Table tbl) {
			bynum.put(tbl.num, tbl);
			byname.put(tbl.name, tbl);
		}

		Tables build() {
			return new Tables(bynum.build(), byname.build());
		}
	}

}
