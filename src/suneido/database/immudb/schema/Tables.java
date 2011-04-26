/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import javax.annotation.concurrent.Immutable;

import suneido.util.PersistentMap;

@Immutable
public class Tables {
	private final PersistentMap<Integer, Table> bynum;
	private final PersistentMap<String, Table> byname;
//	public final PersistentMap<Index, ImmutableList<ForeignKey>> fkdsts;

	public Tables() {
		this.bynum = PersistentMap.empty();
		this.byname = PersistentMap.empty();
	}

	private Tables(PersistentMap<Integer, Table> bynum,
			PersistentMap<String, Table> byname) {
		this.bynum = bynum;
		this.byname = byname;
	}

	public Table get(int tblnum) {
		return bynum.get(tblnum);
	}

	public Table get(String tblname) {
		return byname.get(tblname);
	}

	public Tables with(Table tbl) {
		return new Tables(bynum.with(tbl.num, tbl), byname.with(tbl.name, tbl));
	}

	public Tables without(Table tbl) {
		// look up old name to handle rename
		Table old = bynum.get(tbl.num);
		if (old == null)
			return this;
		return new Tables(bynum.without(tbl.num), byname.without(old.name));
	}

	public static class Builder {
		private final PersistentMap.Builder<Integer, Table> bynum =
				PersistentMap.builder();
		private final PersistentMap.Builder<String, Table> byname =
				PersistentMap.builder();

		public void add(Table tbl) {
			bynum.put(tbl.num, tbl);
			byname.put(tbl.name, tbl);
		}

		public Tables build() {
			return new Tables(bynum.build(), byname.build());
		}
	}

}