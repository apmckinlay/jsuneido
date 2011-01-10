/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

/**
 * Stores a list of keys in sorted order in a ByteBuffer using {@link RecordBase}
 * Keys are nested records.
 * Pointers are {@link MmapFile} int's
 */
@Immutable
public class BtreeDbNode extends BtreeNode {
	RecordBase<Record> rec;

	public static BtreeDbNode leaf(ByteBuffer buf) {
		return new BtreeDbNode(Type.LEAF, buf);
	}

	public static BtreeDbNode tree(ByteBuffer buf) {
		return new BtreeDbNode(Type.TREE, buf);
	}

	public BtreeDbNode(Type type, ByteBuffer buf) {
		super(type);
		rec = new RecordBase<Record>(buf, 0);
	}

	private BtreeDbNode(Type type, RecordBuilder rb) {
		this(type, rb.asByteBuffer());
	}

	public static BtreeDbNode of(Type type, Object a) {
		return new BtreeDbNode(type, new RecordBuilder().add(a));
	}

	public static BtreeDbNode of(Type type, Object a, Object b) {
		return new BtreeDbNode(type, new RecordBuilder().add(a).add(b));
	}

	public static BtreeDbNode of(Type type, Object a, Object b, Object c) {
		return new BtreeDbNode(type, new RecordBuilder().add(a).add(b).add(c));
	}

	@Override
	public int size() {
		return rec.size();
	}

	@Override
	public BtreeNode with(Record key) {
		return new BtreeMemNode(this).with(key);
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		return new Record(rec.buf, rec.offset + rec.fieldOffset(i));
	}

	@Override
	public Record find(Record key) {
		int at = lowerBound(key.buf, key.offset);
		Record slot = get(at);
		if (type == Type.LEAF)
			return at < size() ? slot : null;
		else
			return slot.startsWith(key) ? slot : get(at - 1);
	}

	protected int lowerBound(ByteBuffer kbuf, int koff) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (Record.compare(rec.buf, rec.fieldOffset(middle), kbuf, koff) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	@Override
	public Split split(Record key, int adr) {
		BtreeNode left;
		BtreeNode right;
		Record splitKey;
		int keyPos = lowerBound(key.buf, key.offset);
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = BtreeDbNode.of(type, key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
			if (keyPos <= mid) {
				left = new BtreeMemNode(type, rec.buf)
						.add(this, 0, keyPos).add(key).add(this, keyPos, mid);
				right = new BtreeMemNode(type, rec.buf)
						.add(this, mid, size());
			} else {
				left = new BtreeMemNode(type, rec.buf)
						.add(this, 0, mid);
				right = new BtreeMemNode(type, rec.buf)
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

//	public int persist() {
//		// TODO persist children
//		return persistRecord();
//	}

}
