/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Strings;

/**
 * Parent type for {@link BtreeDbNode}, {@link BtreeMemNode}.
 * Provides access to a list of keys in sorted order.
 * Keys are {@link Record}'s.
 * The last field on leaf keys is a pointer to the corresponding data record.
 * A tree node key is a leaf key plus a pointer to the child node.
 * Pointers are {@link MmapFile} adr int's
 * <p>
 * The first/leftmost key in tree nodes is always "minimal",
 * less than any real key.
 * <p>
 * Tree keys lead to leaf keys greater than themselves.
 * In unique btrees tree keys have their data address set to MAXADR
 * so updates (or remove/add) keep key in same node.
 * <p>
 */
@Immutable
abstract class BtreeNode implements Storable {
	/** level = 0 for leaf, level = treeLevels for root */
	protected final int level;

	BtreeNode(int level) {
		assert level >= 0;
		this.level = level;
	}

	static BtreeNode emptyLeaf() {
		return emptyNode(0);
	}

	static BtreeNode emptyNode(int level) {
		return new BtreeMemNode(level);
	}

	boolean isLeaf() {
		return level == 0;
	}

	boolean isTree() {
		return level != 0;
	}

	abstract int size();

	boolean isEmpty() {
		return size() == 0;
	}

	/** Inserts key in order */
	abstract BtreeNode with(Record key);

	/** @return null if key not found */
	BtreeNode without(Record key) {
		assert isLeaf();
		if (isEmpty())
			return null;
		int at = lowerBound(key);
		if (at >= size() || ! get(at).equals(key))
			return null; // key not found
		return without(at);
	}

	abstract BtreeNode without(int i);

	abstract BtreeNode minimizeLeftMost();

	/** used by split */
	abstract BtreeNode slice(int from, int to);

	/** used by split, either from will be 0 or to will be size */
	abstract BtreeNode without(int from, int to);

	abstract Record get(int i);

	abstract int store2(Storage stor);

	Record find(Record key) {
		int at = findPos(key);
		return (at < 0 || at >= size()) ? null : get(at);
	}

	int findPos(Record key) {
		int at = lowerBound(key);
		return isLeaf() ? at : Math.max(0, at - 1);
	}

	int lowerBound(Record key) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (compare(middle, key) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	private int compare(int middle, Record key) {
		return get(middle).compareTo(key);
	}

	Record first() {
		return get(0);
	}

	Record last() {
		return get(size() - 1);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(printName()).append(" ");
		if (isLeaf())
			sb.append("leaf");
		else
			sb.append("level=").append(level);
		sb.append(" size=").append(size());
		sb.append(" [");
		for (int i = 0; i < size(); ++i)
			sb.append(get(i));
		sb.append("]");
		return sb.toString();
	}

	void print2(Writer w, Storage stor) throws IOException {
		String indent = Strings.repeat("     ", level);
		w.append(indent).append(printName() + " level " + level + "\n");
		for (int i = 0; i < size(); ++i) {
			Record slot = get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0)
				Btree2.childNode(stor, level - 1, slot).print2(w, stor);
		}
	}

	abstract String printName();

	/** returns the number of nodes processed */
	int check2(Tran tran, Record from, Record to) {
		for (int i = 1; i < size(); ++i)
			assert get(i - 1).compareTo(get(i)) < 0;
		if (isLeaf())
			return checkLeaf(tran, from, to);
		else
			return checkTree(tran, from, to);
	}


	private int checkTree(Tran tran, Record from, Record to) {
		assert isMinimalKey(get(0)) : "minimal";
		if (size() > 1)
			assert from.compareTo(get(1)) <= 0;
		assert to == null || to.compareTo(get(size() - 1)) > 0;
		int nnodes = 1;
		to = 1 < size() ? get(1) : null;
		nnodes += Btree2.childNode(tran.istor, level - 1, get(0)).check2(tran, from, to);
		for (int i = 1; i < size(); ++i) {
			from = get(i);
			to = i + 1 < size() ? get(i + 1) : null;
			nnodes += Btree2.childNode(tran.istor, level - 1, from).check2(tran, from, to);
		}
		return nnodes;
	}

	private int checkLeaf(Tran tran, Record from, Record to) {
		if (! isEmpty()) {
			from = new RecordBuilder().addPrefix(from, from.size() - 1).build();
			assert get(0).compareTo(from) > 0
					: "first " + get(0) + " NOT > " + from;
			if (to != null ) {
				to = new RecordBuilder().addPrefix(to, to.size() - 1).build();
				assert get(size() - 1).compareTo(to) <= 0
						: "last " + get(size() - 1) + " NOT <= " + to;
			}
		}
		for (int i = 1; i < size(); ++i) {
			int adr = adr(get(i));
			if (IntRefs.isIntRef(adr))
				assert tran.intToRef(adr) instanceof Record;
		}
		return 1;
	}

	protected static boolean isMinimalKey(Record key) {
		int keySize = key.size();
		for (int i = 0; i < keySize - 1; ++i)
			if (key.fieldLength(i) > 0)
				return false;
		return true;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (! (other instanceof BtreeNode))
			return false;
		BtreeNode that = (BtreeNode) other;
		if (this.size() != that.size())
			return false;
		for (int i = 0; i < size(); ++i)
			if (! this.get(i).equals(that.get(i)))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	protected static Record minimize(Record key) {
		return key.childRef() == null
				? minimalKey(key.size(), adr(key))
				: minimalKey(key.size(), key.childRef());
	}

	protected static int adr(Record rec) {
		return (int) rec.getLong(rec.size() - 1);
	}

	protected static Record minimalKey(int nfields, int adr) {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < nfields - 1; ++i)
			rb.add("");
		rb.adduint(adr);
		return rb.build();
	}

	protected static Record minimalKey(int nfields, Storable childRef) {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < nfields - 1; ++i)
			rb.add("");
		rb.addRef(childRef);
		return rb.build();
	}

	abstract BtreeNode withUpdate(int i, BtreeNode child);

	abstract void freeze();

	abstract boolean frozen();
}
