/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

import com.google.common.base.Strings;

/**
 * Stores a list of keys in sorted order in a ByteBuffer using {@link RecordBase}
 * Keys are nested records.
 * Pointers are integers as used by {@link MmapFile}
 */
@Immutable
public class BtreeNode extends RecordBase<Record> {
	public static final BtreeNode EMPTY_LEAF = new BtreeNode(Type.LEAF, emptyRecBuf);
	private enum Type { LEAF, TREE };
	private final Type type;

	public static BtreeNode leaf(ByteBuffer buf) {
		return new BtreeNode(Type.LEAF, buf);
	}

	public static BtreeNode tree(ByteBuffer buf) {
		return new BtreeNode(Type.TREE, buf);
	}

	public BtreeNode(Type type, ByteBuffer buf) {
		super(buf, 0);
		this.type = type;
	}

	private BtreeNode(Type type, RecordBuilder rb) {
		this(type, rb.asByteBuffer());
	}

	public static BtreeNode of(Type type, Object a) {
		return new BtreeNode(type, new RecordBuilder().add(a));
	}

	public static BtreeNode of(Type type, Object a, Object b) {
		return new BtreeNode(type, new RecordBuilder().add(a).add(b));
	}

	public static BtreeNode of(Type type, Object a, Object b, Object c) {
		return new BtreeNode(type, new RecordBuilder().add(a).add(b).add(c));
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		return new Record(buf, offset + getOffset(i));
	}

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	The first key greater than or equal to the one specified
	 * 			or null if there isn't one.
	 */
	public Record find(Record key) {
		int at = lowerBound(key.buf, key.offset, key.length());
		Record slot = get(at);
		if (type == Type.LEAF)
			return at < size() ? slot : null;
		else
			return slot.startsWith(key) ? slot : get(at - 1);
	}

	public BtreeNode with(Record key) {
		int at = lowerBound(key.buf, key.offset, key.length());
		return withAt(key, at);
	}

	private BtreeNode withAt(Record key, int at) {
		return BtreeNode.of(type,
				new RecordSlice(this, 0, at),
				key,
				new RecordSlice(this, at, size() - at));
	}

	Split split(Record key, int adr) {
		BtreeNode left;
		BtreeNode right;
		Record splitKey;
		int keyPos = lowerBound(key.buf, key.offset, key.length());
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = BtreeNode.of(type, key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
			if (keyPos <= mid) {
				left = BtreeNode.of(type,
						new RecordSlice(this, 0, keyPos),
						key,
						new RecordSlice(this, keyPos, mid - keyPos));
				right = BtreeNode.of(type, new RecordSlice(this, mid, size() - mid));
			} else {
				left = BtreeNode.of(type, new RecordSlice(this, 0, mid));
				right = BtreeNode.of(type,
						new RecordSlice(this, mid, keyPos - mid),
						key,
						new RecordSlice(this, keyPos, size() - keyPos));
			}
			Tran.redir(adr, left);
		}
		int splitKeySize = splitKey.size();
		if (type == Type.TREE)
			--splitKeySize;
		int rightAdr = Tran.refToInt(right);
		splitKey = Record.of(
				new RecordSlice(splitKey, 0, splitKeySize),
				rightAdr);
		return new Split(adr, rightAdr, splitKey);
	}

	public int persist() {
		// TODO persist children
		return persistRecord();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append("[");
		for (int i = 0; i < size(); ++i)
			sb.append(get(i));
		sb.append("]");
		return sb.toString();
	}

	void print(Writer w, int level) throws IOException {
		String indent = Strings.repeat("     ", level);
		w.append(indent).append(type.toString()).append("\n");
		for (int i = 0; i < size(); ++i) {
			Record slot = get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0) {
				int adr = (Integer) slot.get(slot.size() - 1);
				BtreeNode node = (level == 1)
					? Btree.leafNodeAt(adr) : Btree.treeNodeAt(adr);
				node.print(w, level - 1);
			}
		}
	}

	protected int lowerBound(ByteBuffer kbuf, int koff, int klen) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (Record.compare(buf, getOffset(middle), kbuf, koff) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	public static BtreeNode newRoot(Split split) {
		RecordBuilder key1 = new RecordBuilder();
		for (int i = 0; i < split.key.size() - 1; ++i)
			key1.add("");
		key1.add(split.left);
		return BtreeNode.of(Type.TREE, key1.build(), split.key);
	}

}
