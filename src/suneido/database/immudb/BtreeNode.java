/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

import com.google.common.base.Strings;

/**
 * Parent type for {@link BtreeDbNode}, {@link BtreeDbMemNode},
 * and {@link BtreeMemNode}
 * Provides access to a list of keys in sorted order.
 * Keys are {@link Record}'s.
 * The last field on leaf keys is a pointer to the corresponding data record.
 * A tree node key is a leaf key plus a pointer to the child node.
 * Pointers are {@link MmapFile} adr int's
 */
@Immutable
public abstract class BtreeNode {
	protected final int level;

	public BtreeNode(int level) {
		this.level = level;
	}

	public static BtreeNode emptyLeaf() {
		return emptyNode(0);
	}

	public static BtreeNode emptyNode(int level) {
		return new BtreeMemNode(level);
	}

	public int level() {
		return level;
	}

	public boolean isLeaf() {
		return level == 0;
	}

	public abstract int size();

	public boolean isEmpty() {
		return size() == 0;
	}

	/** Inserts key in order */
	public abstract BtreeStoreNode with(Record key);

	/** @return null if key not found */
	public BtreeNode without(Record key) {
		if (isEmpty())
			return null;
		int at = lowerBound(key);
		if (isLeaf()) {
			if (at >= size() || ! get(at).equals(key))
				return null; // key not found
		} else { // tree node
			if (at >= size() || ! get(at).startsWith(key))
				--at;
			assert at >= 0;
		}
		return without(at);
	}

	protected abstract BtreeNode without(int i);

	/** used by split */
	public abstract BtreeNode slice(int from, int to);

	/** used by split, either from will be 0 or to will be size */
	public abstract BtreeNode without(int from, int to);

	public abstract Record get(int i);

	public abstract int store(Tran tran);

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	For leaf nodes, the first key >= the one specified,
	 * 			or null if there isn't one.
	 * 			For tree nodes, the first key <= the one specified,
	 *			or the first key.
	 */
	public Record find(Record key) {
		int at = findPos(key);
		return (at < 0 || at >= size()) ? null : get(at);
	}

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	For leaf nodes, the first position >= the one specified,
	 * 			or -1 if there isn't one.position <= the one specified,
	 *			or the first key.
	 */
	public int findPos(Record key) {
		if (isEmpty())
			return -1;
		int at = lowerBound(key);
		if (isLeaf())
			return at < size() ? at : -1;
		else {
			if (at == 0)
				return at;
			if (at >= size())
				return at - 1;
			Record slot = get(at);
			return slot.startsWith(key) ? at : at - 1;
		}
	}

	public int lowerBound(Record key) {
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

	public Split split(Tran tran, Record key, int adr) {
		int level = level();
		BtreeNode right;
		Record splitKey;
		int keyPos = lowerBound(key);
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = new BtreeMemNode(level, key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
			BtreeNode left;
			right = slice(mid, size());
			left = without(mid, size());
			if (keyPos <= mid)
				left = left.with(key);
			else
				right = right.with(key);
			tran.redir(adr, left);
		}
		int splitKeySize = splitKey.size();
		if (level > 0) // tree node
			--splitKeySize;
		int rightAdr = tran.refToInt(right);
		splitKey = new RecordBuilder().addPrefix(splitKey, splitKeySize)
				.add(rightAdr).build();
		return new Split(level, adr, rightAdr, splitKey);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BtreeNode ");
		if (isLeaf())
			sb.append("leaf");
		else
			sb.append("level=").append(level());
		sb.append(" size=").append(size());
		sb.append(" [");
		for (int i = 0; i < size(); ++i)
			sb.append(get(i));
		sb.append("]");
		return sb.toString();
	}

	public void print(Writer w, Tran tran, int at) throws IOException {
		int level = level();
		String indent = Strings.repeat("     ", level);
		w.append(indent).append("NODE @ " + at + "\n");
		for (int i = 0; i < size(); ++i) {
			Record slot = get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0) {
				int adr = ((Number) slot.get(slot.size() - 1)).intValue();
				Btree.nodeAt(tran, level - 1, adr).print(w, tran, adr);
			}
		}
	}

	/** returns the number of nodes processed */
	int check(Tran tran, Record key) {
		for (int i = 1; i < size(); ++i)
			assert get(i - 1).compareTo(get(i)) < 0;
		if (isLeaf()) {
			if (! isEmpty()) {
				key = new RecordBuilder().addPrefix(key, key.size() - 1).build();
				assert key.compareTo(get(0)) <= 0;
			}
			return 1;
		}
		assert isMinimalKey(get(0)) : "minimal";
		if (size() > 1)
			assert key.compareTo(get(1)) <= 0;
		int adr = Btree.getAddress(get(0));
		int nnodes = 1;
		nnodes += Btree.nodeAt(tran, level - 1, adr).check(tran, key);
		for (int i = 1; i < size(); ++i) {
			Record key2 = get(i);
			adr = Btree.getAddress(key2);
			nnodes += Btree.nodeAt(tran, level - 1, adr).check(tran, key2);
		}
		return nnodes;
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

	protected static Record minimize(Record key) {
		int ptr = pointer(key);
		return minimalKey(key.size(), ptr);
	}

	protected static int pointer(Record rec) {
		return ((Number) rec.get(rec.size() - 1)).intValue();
	}

	protected static Record minimalKey(int nfields, int ptr) {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < nfields - 1; ++i)
			rb.add("");
		rb.add(ptr);
		return rb.build();
	}

}
