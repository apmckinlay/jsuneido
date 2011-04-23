/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.Translator;

/**
 * Normally immutable persistent trees are "updated" by copying & updating
 * nodes all the way up to the root.
 * To avoid writing so many nodes to disk, we save "redirections".
 * e.g. if we "update" a leaf, instead of updating it's parents
 * we just "redirect" the leaf's old address to it's new address.
 * <p>
 * Note: Redirections are stored in {@link DbHashTrie}
 * so it can't use this optimization.
 */
public class Redirects {
	private final DbHashTrie original;
	private DbHashTrie redirs;
	boolean noneAdded = true;

	public Redirects(DbHashTrie redirs) {
		original = redirs;
		this.redirs = redirs;
	}

	public void put(int from, int to) {
		assert ! IntRefs.isIntRef(from);
		redirs = redirs.with(new IntEntry(from, to));
		noneAdded = false;
	}

	public int get(int from) {
		Entry e = redirs.get(from);
		return e == null ? from : ((IntEntry) e).value;
	}

	public int store(Translator translator) {
		return redirs.store(translator);
	}

	public boolean noneAdded() {
		return noneAdded;
	}

	public void print() {
		redirs.print();
	}

	/** for tests */
	DbHashTrie redirs() {
		return redirs;
	}

	public void merge(DbHashTrie current) {
		if (current == original)
			return; // no concurrent changes to merge

		Proc proc = new Proc(original, current);
		redirs.traverseChanges(proc);
		redirs = proc.merged;
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
		public void apply(Entry e) {
			int adr = ((IntEntry) e).key;
			if (original.get(adr) != current.get(adr))
				throw conflict;
			merged = merged.with(e);
		}

	}

	public static class Conflict extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	// preconstructed for performance
	private static Conflict conflict = new Conflict();

}
