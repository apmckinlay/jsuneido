/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.Bootstrap.TN;
import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.Translator;
import suneido.database.immudb.UpdateTransaction.Conflict;
import suneido.util.ParallelIterable;

import com.google.common.collect.ImmutableList;

@NotThreadSafe
public class DbInfo {
	private final Storage stor;
	private DbHashTrie dbinfo;

	public DbInfo(Storage stor) {
		this.stor = stor;
		dbinfo = DbHashTrie.empty(stor);
	}

	public DbInfo(Storage stor, int adr) {
		this.stor = stor;
		dbinfo = DbHashTrie.from(stor, adr);
	}

	public DbInfo(Storage stor, DbHashTrie dbinfo) {
		this.stor = stor;
		this.dbinfo = dbinfo;
	}

	DbHashTrie dbinfo() {
		return dbinfo;
	}

	public TableInfo get(int tblnum) {
		Entry e = dbinfo.get(tblnum);
		if (e instanceof IntEntry) {
			int adr = ((IntEntry) e).value;
			Record rec = new Record(stor.buffer(adr));
			TableInfo ti = new TableInfo(rec);
			dbinfo = dbinfo.with(ti);
			return ti;
		} else
			return (TableInfo) e;
	}

	public void add(TableInfo ti) {
		dbinfo = dbinfo.with(ti);
	}

	/** ++nrows, totalsize += size */
	public void addrow(int tblnum, int size) {
		TableInfo ti = get(tblnum);
		if (tblnum <= TN.INDEXES && ti == null) // bootstrap
			return;
		TableInfo ti2 = ti.with(size);
		if (ti2 != ti)
			dbinfo = dbinfo.with(ti2);
	}

	public int store() {
		return dbinfo.store(new DbInfoTranslator());
	}

	private class DbInfoTranslator implements Translator {
		@Override
		public int translate(Entry entry) {
			if (entry instanceof TableInfo)
				return ((TableInfo) entry).store(stor);
			else
				return ((IntEntry) entry).value;
		}
	}

	public void merge(DbHashTrie original, DbHashTrie current) {
		if (current == original)
			return; // no concurrent changes to merge

		Proc proc = new Proc(original, current);
		dbinfo.traverseChanges(proc);
		dbinfo = proc.merged;
	}

	private static class Proc implements DbHashTrie.Process {
		private final DbHashTrie original;
		private final DbHashTrie current;
		private DbHashTrie merged;

		public Proc(DbHashTrie original, DbHashTrie current) {
			this.original = original;
			this.current = current;
			merged = current;
		}

		@Override
		public void apply(Entry entry) {
			TableInfo ours = (TableInfo) entry;
			TableInfo orig = (TableInfo) original.get(ours.tblnum);
			TableInfo cur = (TableInfo) current.get(ours.tblnum);
			merged = merged.with(merge(ours, orig, cur));
		}

		@SuppressWarnings("unchecked")
		TableInfo merge(TableInfo ours, TableInfo orig, TableInfo cur) {
			assert ours.tblnum == orig.tblnum && ours.tblnum == cur.tblnum;
			assert ours.nextfield == orig.nextfield && ours.nextfield == cur.nextfield;
			ImmutableList.Builder<IndexInfo> builder = ImmutableList.builder();
			for (List<IndexInfo> idxs :
					ParallelIterable.of(ours.indexInfo, orig.indexInfo, cur.indexInfo)) {
				IndexInfo ourIdx = idxs.get(0);
				IndexInfo origIdx = idxs.get(1);
				IndexInfo curIdx = idxs.get(2);
				if (origIdx == curIdx) // no one else has changed it
					builder.add(ourIdx);
				else if (ourIdx == origIdx) // we didn't change it
					builder.add(curIdx);
				else
					builder.add(merge(ourIdx, origIdx, curIdx));
			}
			int nrows = cur.nrows() + (ours.nrows() - orig.nrows());
			long totalsize = cur.totalsize() + (ours.totalsize() - orig.totalsize());
			return new TableInfo(cur.tblnum, cur.nextfield, nrows, totalsize,
					builder.build());
		}

		private IndexInfo merge(IndexInfo ours, IndexInfo orig, IndexInfo cur) {
			assert ours.columns.equals(orig.columns);
			assert orig.columns.equals(cur.columns);
			int root, treeLevels;
			if (orig.root == cur.root) { // no one else has changed it
				assert orig.treeLevels == cur.treeLevels;
				root = ours.root;
				treeLevels = ours.treeLevels;
			} else if (ours.root == orig.root) { // we didn't changes it
				assert ours.treeLevels == orig.treeLevels;
				root = cur.root;
				treeLevels = cur.treeLevels;
			} else
				throw conflict;
			int nnodes = cur.nnodes + (ours.nnodes - orig.nnodes);
			return new IndexInfo(cur.columns, root, treeLevels, nnodes);
		}

	}

	private static final Conflict conflict =
			new Conflict("concurrent index modification");

}
