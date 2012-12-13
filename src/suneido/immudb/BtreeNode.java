/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.concurrent.Immutable;

import suneido.util.CommaStringBuilder;

import com.google.common.base.Strings;

/**
 * Parent type for {@link BtreeDbNode} and {@link BtreeMemNode}.
 * Provides access to a list of {@link BtreeKey}'s in sorted order.
 * <p>
 * The first/leftmost key in tree nodes is always "minimal",
 * i.e. less than any real key.
 * <p>
 * Tree keys lead to leaf keys greater than themselves.
 * In unique btrees tree keys have their data address set to MAXADR
 * so updates (or remove/add) keep key in same node.
 */
@Immutable
abstract class BtreeNode {
	/** level = 0 for leaf, level = treeLevels for root */
	protected final int level;

	protected BtreeNode(int level) {
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
	abstract BtreeNode with(BtreeKey key);

	/** @return null if key not found */
	BtreeNode without(BtreeKey key) {
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

	abstract BtreeKey get(int i);

	abstract BtreeDbNode store(Storage stor);

	abstract int address();

	BtreeKey find(BtreeKey key) {
		int at = findPos(key);
		return (at < 0 || at >= size()) ? null : get(at);
	}

	int findPos(BtreeKey key) {
		int at = lowerBound(key);
		return isLeaf() ? at : Math.max(0, at - 1);
	}

	int lowerBound(BtreeKey key) {
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

	private int compare(int middle, BtreeKey key) {
		return get(middle).compareTo(key);
	}

	BtreeKey first() {
		return get(0);
	}

	BtreeKey last() {
		return get(size() - 1);
	}

	@Override
	public String toString() {
		CommaStringBuilder sb = new CommaStringBuilder();
		sb.append(printName()).append(" ");
		if (isLeaf())
			sb.append("leaf");
		else
			sb.append("level=").append(level);
		sb.append(" size=").append(size());
		sb.append(" [");
		for (int i = 0; i < size(); ++i)
			sb.add(get(i));
		sb.append("]");
		return sb.toString();
	}

	void print(Writer w, Storage stor) throws IOException {
		String indent = Strings.repeat("     ", level);
		w.append(indent).append(printName() + " level " + level + "\n");
		for (int i = 0; i < size(); ++i) {
			BtreeKey slot = get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0)
				childNode(stor, i).print(w, stor);
		}
	}

	abstract String printName();

	/** returns the number of nodes processed */
	int check(Tran tran, BtreeKey from, BtreeKey to) {
		for (int i = 1; i < size(); ++i)
			assert get(i - 1).compareTo(get(i)) < 0;
		if (isLeaf())
			return checkLeaf(tran, from, to);
		else
			return checkTree(tran, from, to);
	}

	private int checkTree(Tran tran, BtreeKey from, BtreeKey to) {
		assert get(0).isMinimalKey() : "not minimal " + get(0);
		if (size() > 1)
			assert from.compareTo(get(1)) <= 0;
		assert to == null || to.compareTo(get(size() - 1)) > 0;
		int nnodes = 1;
		to = 1 < size() ? get(1) : null;
		nnodes += childNode(tran.istor, 0).check(tran, from, to);
		for (int i = 1; i < size(); ++i) {
			from = get(i);
			to = i + 1 < size() ? get(i + 1) : null;
			nnodes += childNode(tran.istor, i).check(tran, from, to);
		}
		return nnodes;
	}

	private int checkLeaf(Tran tran, BtreeKey from, BtreeKey to) {
		if (! isEmpty()) {
			from = new BtreeKey(from.key);
			assert get(0).compareTo(from) > 0
					: "first " + get(0) + " NOT > " + from;
			if (to != null )
				assert get(size() - 1).compareTo(to) <= 0
						: "last " + get(size() - 1) + " NOT <= " + to;
		}
		for (int i = 1; i < size(); ++i) {
			int adr = get(i).adr();
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

	protected static BtreeTreeKey minimalKey(int nfields, BtreeNode child) {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < nfields; ++i)
			rb.add("");
		return rb.btreeTreeKey(child);
	}

	abstract BtreeNode withUpdate(int i, BtreeNode child);

	abstract void freeze();

	abstract boolean frozen();

	abstract BtreeNode childNode(Storage stor, int i);

}
