/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import suneido.database.immudb.Btree.Split;

import com.google.common.base.Strings;

/**
 * Common interface for {@link BtreeDbNode} and {@link BtreeMemNode}
 * Provides access to a list of keys in sorted order {@link Record}
 * Keys are {@link Record}'s.
 * The final field on leaf keys is a pointer to the corresponding data record.
 * A tree keys is a leaf key plus a pointer to the child node.
 * Pointers are {@link MmapFile} int's
 */
public abstract class BtreeNode {

	public abstract int level();

	public abstract int size();

	public abstract ByteBuffer buf();

	/** Inserts key in order */
	public abstract BtreeNode with(Record key);

	public abstract Record get(int i);

	public abstract ByteBuffer fieldBuf(int i);

	public abstract int fieldOffset(int i);

	public abstract int store(Tran tran);

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	The first key greater than or equal to the one specified
	 * 			or null if there isn't one.
	 */
	public Record find(Record key) {
		int at = lowerBound(key.buf, key.offset);
		Record slot = get(at);
		if (level() == 0) // leaf
			return at < size() ? slot : null;
		else
			return slot.startsWith(key) ? slot : get(at - 1);
	}

	public int lowerBound(ByteBuffer kbuf, int koff) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (Record.compare(fieldBuf(middle), fieldOffset(middle),
					kbuf, koff) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	public Split split(Tran tran, Record key, int adr) {
		int level = level();
		BtreeNode left;
		BtreeNode right;
		Record splitKey;
		int keyPos = lowerBound(key.buf, key.offset);
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = new BtreeMemNode(level).add(key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
			if (keyPos <= mid) {
				left = new BtreeMemNode(level, buf())
						.add(this, 0, keyPos).add(key).add(this, keyPos, mid);
				right = new BtreeMemNode(level, buf())
						.add(this, mid, size());
			} else {
				left = new BtreeMemNode(level, buf())
						.add(this, 0, mid);
				right = new BtreeMemNode(level, buf())
						.add(this, mid, keyPos).add(key).add(this, keyPos, size());
			}
			tran.redir(adr, left);
		}
		int splitKeySize = splitKey.size();
		if (level > 0) // tree node
			--splitKeySize;
		int rightAdr = tran.refToInt(right);
		splitKey = new RecordBuilder().add(splitKey).add(rightAdr).build();
		return new Split(level, adr, rightAdr, splitKey);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BtreeNode level=").append(level());
		sb.append(" [");
		for (int i = 0; i < size(); ++i)
			sb.append(get(i));
		sb.append("]");
		return sb.toString();
	}

	public void print(Writer w, Tran tran) throws IOException {
		int level = level();
		String indent = Strings.repeat("     ", level);
		w.append(indent).append("NODE\n");
		for (int i = 0; i < size(); ++i) {
			Record slot = get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0) {
				int adr = ((Number) slot.get(slot.size() - 1)).intValue();
				Btree.nodeAt(tran, level - 1, adr).print(w, tran);
			}
		}
	}

}
