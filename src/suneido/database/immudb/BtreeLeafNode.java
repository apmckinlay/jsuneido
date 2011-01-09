/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.Util.lowerBound;

import java.nio.ByteBuffer;

import suneido.database.immudb.Btree.Split;

/**
 * Keys have the data record address as the last field.
 * This ensures uniqueness.
 * Has next and prev pointers stored immediately following the record.
 * Normally immutable.
 */
public class BtreeLeafNode extends BtreeNode {
	public static final BtreeLeafNode EMPTY = new BtreeLeafNode(emptyRecBuf);

	public BtreeLeafNode(ByteBuffer buf) {
		super(buf);
	}

	private BtreeLeafNode(RecordBuilder rb) {
		this(rb.asByteBuffer());
	}

	public static BtreeLeafNode of(Object a) {
		return new BtreeLeafNode(new RecordBuilder().add(a));
	}

	public static BtreeLeafNode of(Object a, Object b) {
		return new BtreeLeafNode(new RecordBuilder().add(a).add(b));
	}

	public static BtreeLeafNode of(Object a, Object b, Object c) {
		return new BtreeLeafNode(new RecordBuilder().add(a).add(b).add(c));
	}

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	The first key greater than or equal to the one specified
	 * 			or null if there isn't one.
	 */
	public Record find(Record key) {
		int at = lowerBound(this, key);
		return at < size() ? get(at) : null;
	}

	public BtreeLeafNode with(Record key) {
		int at = lowerBound(this, key);
		return withAt(key, at);
	}

	private BtreeLeafNode withAt(Record key, int at) {
		return BtreeLeafNode.of(
				new RecordSlice(this, 0, at),
				key,
				new RecordSlice(this, at, size() - at));
	}

	// identical to BtreeTreeNode.split except for adding splitKey address
	Split split(Record key, int adr) {
		BtreeLeafNode left;
		BtreeLeafNode right;
		Record splitKey;
		int keyPos = lowerBound(this, key);
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = BtreeLeafNode.of(key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
			if (keyPos <= mid) {
				left = BtreeLeafNode.of(
						new RecordSlice(this, 0, keyPos),
						key,
						new RecordSlice(this, keyPos, mid - keyPos));
				right = BtreeLeafNode.of(new RecordSlice(this, mid, size() - mid));
			} else {
				left = BtreeLeafNode.of(new RecordSlice(this, 0, mid));
				right = BtreeLeafNode.of(
						new RecordSlice(this, mid, keyPos - mid),
						key,
						new RecordSlice(this, keyPos, size() - keyPos));
			}
			Tran.redir(adr, left);
		}
		int rightAdr = Tran.refToInt(right);
		splitKey = Record.of(
				new RecordSlice(splitKey, 0, splitKey.size()),
				rightAdr);
		return new Split(adr, rightAdr, splitKey);
	}

}
