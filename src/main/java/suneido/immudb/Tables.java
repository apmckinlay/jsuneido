/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import suneido.util.PersistentMap;

import com.google.common.collect.Maps;

/**
 * Stores the database schema in memory.
 */
@Immutable
class Tables {
	private final PersistentMap<Integer, Table> bynum;
	private final PersistentMap<String, Table> byname;
	private final ForeignKeyTargets fkdsts;
	final int maxTblnum;

	Tables() {
		bynum = PersistentMap.empty();
		byname = PersistentMap.empty();
		fkdsts = ForeignKeyTargets.empty();
		maxTblnum = 0;
	}

	private Tables(PersistentMap<Integer, Table> bynum,
			PersistentMap<String, Table> byname,
			ForeignKeyTargets fkdsts, int maxTblnum) {
		this.bynum = bynum;
		this.byname = byname;
		this.fkdsts = fkdsts;
		this.maxTblnum = maxTblnum;
	}

	Table get(int tblnum) {
		return bynum.get(tblnum);
	}

	Table get(String tableName) {
		return byname.get(tableName);
	}

	Tables with(Table tbl) {
		ForeignKeyTargets fkd = fkdsts;
		for (Index idx : tbl.indexes) {
			ForeignKeySource fksrc = idx.fksrc;
			if (fksrc == null)
				continue;
			fkd = fkd.with(fksrc,
					new ForeignKeyTarget(tbl.num, tbl.name, idx.colNums, fksrc.mode));
		}
		return new Tables(bynum.with(tbl.num, tbl), byname.with(tbl.name, tbl),
				fkd, Math.max(tbl.num, maxTblnum));
	}

	Tables without(Table tbl) {
		// look up old name to handle rename
		Table old = bynum.get(tbl.num);
		if (old == null)
			return this;
		ForeignKeyTargets fkd = fkdsts;
		for (Index idx : tbl.indexes) {
			ForeignKeySource fksrc = idx.fksrc;
			if (fksrc != null)
				fkd = fkd.without(fksrc,
					new ForeignKeyTarget(tbl.num, tbl.name, idx.colNums, fksrc.mode));
		}
		return new Tables(bynum.without(tbl.num), byname.without(old.name),
				fkd, maxTblnum);
	}

	Set<ForeignKeyTarget> getFkdsts(String tablename, String columns) {
		return fkdsts.get(tablename, columns);
	}

	/** Used by {@link SchemaLoader */
	static class Builder {
		private final PersistentMap.Builder<Integer, Table> bynum =
				PersistentMap.builder();
		private final PersistentMap.Builder<String, Table> byname =
				PersistentMap.builder();
		private final Map<String, Table> tables = Maps.newHashMap();
		int maxTblnum;

		Builder(int maxTblnum) {
			this.maxTblnum = maxTblnum;
		}

		void add(Table tbl) {
			bynum.put(tbl.num, tbl);
			byname.put(tbl.name, tbl);
			if (tbl.num > maxTblnum)
				maxTblnum = tbl.num;
			tables.put(tbl.name, tbl);
		}

		Tables build() {
			return new Tables(bynum.build(), byname.build(), buildFkdsts(), maxTblnum);
		}

		private ForeignKeyTargets buildFkdsts() {
			ForeignKeyTargets.Builder fkdsts = ForeignKeyTargets.builder();
			for (Table tbl : tables.values()) {
				for (Index idx : tbl.indexesList()) {
					ForeignKeySource fksrc = idx.fksrc;
					if (fksrc != null)
						fkdsts.add(fksrc,
							new ForeignKeyTarget(tbl.num, tbl.name, idx.colNums, fksrc.mode));
				}
			 }
			return fkdsts.build();
		}
	}

}
