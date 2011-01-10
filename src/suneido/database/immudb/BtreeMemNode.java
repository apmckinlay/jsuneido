/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.Btree.Split;

/**
 * An in-memory mutable btree node.
 * "updating" a {@link BtreeDbNode} produces a BtreeMemNode
 * <p>To avoid copying allocation, the keys from the original BtreeMemNode
 * are stored as buffer and offset (rather than creating a Record wrapper).
 */
@NotThreadSafe
public class BtreeMemNode extends BtreeNode {
	private final ByteBuffer buf;
	private final int[] data = new int[Btree.MAX_NODE_SIZE];
	private int size;

	public BtreeMemNode(Type type) {
		super(type);
		buf = null;
		size = 0;
	}

	public BtreeMemNode(Type type, ByteBuffer buf) {
		super(type);
		this.buf = buf;
		size = 0;
	}

	public BtreeMemNode(BtreeDbNode dbn) {
		super(dbn.type);
		buf = dbn.rec.buf;
		size = dbn.size();
		for (int i = 0; i < size; ++i)
			data[i] = dbn.rec.fieldOffset(i);
	}

	public static BtreeMemNode emptyLeaf() {
		return new BtreeMemNode(Type.LEAF);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Record get(int i) {
		if (i >= size)
			return Record.EMPTY;
		if (IntRefs.isIntRef(data[i]))
			return (Record) Tran.intToRef(data[i]);
		else
			return new Record(buf, data[i]);
	}

	public BtreeMemNode add(Record key) {
		data[size++] = Tran.refToInt(key);
		return this;
	}

	public BtreeMemNode add(BtreeDbNode dbn, int from, int to) {
		for (int i = from; i < to; ++i)
			data[size++] = dbn.rec.fieldOffset(i);
		return this;
	}

	public BtreeMemNode add(BtreeMemNode dbn, int from, int to) {
		for (int i = from; i < to; ++i)
			data[size++] = dbn.data[i];
		return this;
	}

	@Override
	public BtreeNode with(Record key) {
		int at = lowerBound(key.buf, key.offset);
		System.arraycopy(data, at, data, at + 1, size - at);
		data[at] = Tran.refToInt(key);
		++size;
		return this;
	}

	@Override
	protected int lowerBound(ByteBuffer kbuf, int koff) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			int cmp;
			if (IntRefs.isIntRef(data[middle])) {
				Record key = (Record) Tran.intToRef(data[middle]);
				cmp = Record.compare(key.buf, key.offset, kbuf,  koff);
			} else
				cmp = Record.compare(buf, data[middle], kbuf, koff);
			if (cmp < 0) {
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
		return new BtreeMemNode(Type.TREE).add(key1.build()).add(split.key);
	}

	@Override
	public Split split(Record key, int adr) {
		BtreeNode left;
		BtreeNode right;
		Record splitKey;
		int keyPos = lowerBound(key.buf, key.offset);
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = new BtreeMemNode(type).add(key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
			if (keyPos <= mid) {
				left = new BtreeMemNode(type, buf)
						.add(this, 0, keyPos).add(key).add(this, keyPos, mid);
				right = new BtreeMemNode(type, buf)
						.add(this, mid, size());
			} else {
				left = new BtreeMemNode(type, buf)
						.add(this, 0, mid);
				right = new BtreeMemNode(type, buf)
						.add(this, mid, keyPos).add(key).add(this, keyPos, size());
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

}
