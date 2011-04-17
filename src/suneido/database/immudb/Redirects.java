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
	private DbHashTree redirs;
	boolean isEmpty = true;

	public Redirects(DbHashTree redirs) {
		this.redirs = redirs;
	}

	public void put(int from, int to) {
		assert ! IntRefs.isIntRef(from);
		redirs = redirs.with(from, to);
		isEmpty = false;
	}

	public int get(int from) {
		int to = redirs.get(from);
		return to == 0 ? from : to;
	}

	public int store(Translator translator) {
		return redirs.store(translator);
	}

	public boolean isEmpty() {
		return isEmpty;
	}

	public void print() {
		redirs.print();
	}

}
