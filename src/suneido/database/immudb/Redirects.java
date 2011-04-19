/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Normally immutable persistent trees are "updated" by copying & updating
 * nodes all the way up to the root.
 * To avoid writing so many nodes to disk, we save "redirections".
 * e.g. if we "update" a leaf, instead of updating it's parents
 * we just "redirect" the leaf's old address to it's new address.
 * <p>
 * Note: Redirections are stored in {@link DbHashTree}
 * so it can't use this optimization.
 */
public class Redirects {
	private final DbHashTree original;
	private DbHashTree redirs;
	boolean noneAdded = true;

	public Redirects(DbHashTree redirs) {
		original = redirs;
		this.redirs = redirs;
	}

	public void put(int from, int to) {
		assert ! IntRefs.isIntRef(from);
		redirs = redirs.with(from, to);
		noneAdded = false;
	}

	public int get(int from) {
		int to = redirs.get(from);
		return to == 0 ? from : to;
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
	DbHashTree redirs() {
		return redirs;
	}

	public void merge(DbHashTree current) {
		if (current == original)
			return; // no concurrent changes to merge

		Proc proc = new Proc(original, current);
		redirs.traverseChanges(proc);
		redirs = proc.merged;
	}

	private static class Proc implements DbHashTree.Process {
		private final DbHashTree original;
		private final DbHashTree current;
		private DbHashTree merged;

		public Proc(DbHashTree original, DbHashTree current) {
			this.original = original;
			this.current = current;
			merged = current;
		}

		@Override
		public void apply(int adr, int value) {
			if (original.get(adr) != current.get(adr))
				throw conflict;
			merged = merged.with(adr, value);
		}

	}

	public static class Conflict extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	// preconstructed for performance
	private static Conflict conflict = new Conflict();

}
