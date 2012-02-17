/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.immudb.Bootstrap.TN;
import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.immudb.DbHashTrie.StoredIntEntry;
import suneido.immudb.DbHashTrie.Translator;
import suneido.immudb.UpdateTransaction.Conflict;
import suneido.util.ParallelIterable;

import com.google.common.collect.ImmutableList;

/**
 * Wrapper for read-write access to dbinfo.
 * Thread confined since used by single transactions which are thread confined
 */
@NotThreadSafe
class UpdateDbInfo {
	private final Storage stor;
	private DbHashTrie dbinfo;


	UpdateDbInfo(Storage stor) {
		this(stor, DbHashTrie.empty(stor));
	}

	UpdateDbInfo(Storage stor, DbHashTrie dbinfo) {
		this.stor = stor;
		this.dbinfo = dbinfo;
	}

	void add(TableInfo ti) {
		dbinfo = dbinfo.with(ti);
	}

	TableInfo get(int tblnum) {
		return (TableInfo) dbinfo.get(tblnum);
	}

	DbHashTrie dbinfo() {
		return dbinfo;
	}

	/** update nrows and totalsize */
	void updateRowInfo(int tblnum, int nrows, int size) {
		TableInfo ti = get(tblnum);
		if (tblnum <= TN.INDEXES && ti == null) // bootstrap
			return;
		TableInfo ti2 = ti.with(nrows, size);
		if (ti2 != ti)
			dbinfo = dbinfo.with(ti2);
	}

	int store() {
		return dbinfo.store(new DbInfoTranslator());
	}

	private class DbInfoTranslator implements Translator {
		@Override
		public Entry translate(Entry entry) {
			if (entry instanceof TableInfo) {
				((TableInfo) entry).store(stor);
				return entry;
			} else {
				IntEntry ie = (IntEntry) entry;
				return new StoredIntEntry(ie.key, ie.value);
			}
		}
	}

	void merge(DbHashTrie original, DbHashTrie current) {
		if (current == original)
			return; // no concurrent changes to merge

		MergeProc proc = new MergeProc(original, current);
		dbinfo.traverseChanges(proc);
		dbinfo = proc.merged;
	}

	private static class MergeProc implements DbHashTrie.Process {
		private final DbHashTrie original;
		private final DbHashTrie current;
		private DbHashTrie merged;

		MergeProc(DbHashTrie original, DbHashTrie current) {
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

		private static IndexInfo merge(IndexInfo ours, IndexInfo orig, IndexInfo cur) {
			assert ours.columns == orig.columns;
			assert orig.columns == cur.columns;
			int root, treeLevels;
			if (orig.root == cur.root) { // no one else has changed it
				assert orig.treeLevels == cur.treeLevels;
				root = ours.root;
				treeLevels = ours.treeLevels;
			} else if (ours.root == orig.root) { // we didn't change it
				assert ours.treeLevels == orig.treeLevels;
				root = cur.root;
				treeLevels = cur.treeLevels;
			} else
				throw conflict();
			int nnodes = cur.nnodes + (ours.nnodes - orig.nnodes);
			int totalSize = cur.totalSize + (ours.totalSize - orig.totalSize);
			return new IndexInfo(cur.columns, root, treeLevels, nnodes, totalSize);
		}

	}

	private static Conflict conflict() {
			return new Conflict("concurrent index modification");
	}

}
