/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.Util.lowerBound;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

/**
 * Stores one or more keys.
 * Keys are records.
 * Keys have the data address as the second last field
 * and pointer to child node as the last field.
 * The child node contains keys >= this key.
 * Pointers are long offsets into database file, stored as int using {@link IntLongs}
 */
@Immutable
public class BtreeTreeNode extends BtreeNode {

	public BtreeTreeNode(ByteBuffer buf) {
		super(buf);
	}

	private BtreeTreeNode(RecordBuilder rb) {
		this(rb.asByteBuffer());
	}

	public static BtreeTreeNode of(Object a) {
		return new BtreeTreeNode(new RecordBuilder().add(a));
	}

	public static BtreeTreeNode of(Object a, Object b) {
		return new BtreeTreeNode(new RecordBuilder().add(a).add(b));
	}

	public static BtreeTreeNode of(Object a, Object b, Object c) {
		return new BtreeTreeNode(new RecordBuilder().add(a).add(b).add(c));
	}

	public Record find(Record key) {
		int at = lowerBound(this, key);
		Record slot = get(at);
		return slot.startsWith(key) ? slot : get(at - 1);
	}

	public static BtreeTreeNode newRoot(Split split) {
		RecordBuilder key1 = new RecordBuilder();
		for (int i = 0; i < split.key.size() - 1; ++i)
			key1.add("");
		key1.add(split.left);
		return BtreeTreeNode.of(key1.build(), split.key);
	}

	public BtreeTreeNode with(Record key) {
		int at = lowerBound(this, key);
		return withAt(key, at);
	}

	private BtreeTreeNode withAt(Record key, int at) {
		return BtreeTreeNode.of(
				new RecordSlice(this, 0, at),
				key,
				new RecordSlice(this, at, size() - at));
	}

	public Split split(Record key, int adr) {
		// TODO if key is at end of node, just make new node
		int keyPos = lowerBound(this, key);
		int mid = size() / 2;
		Record midKey = get(mid);
		BtreeTreeNode left;
		BtreeTreeNode right;
		if (keyPos <= mid) {
			left = BtreeTreeNode.of(
					new RecordSlice(this, 0, keyPos),
					key,
					new RecordSlice(this, keyPos, mid - keyPos));
			right = BtreeTreeNode.of(new RecordSlice(this, mid, size() - mid));
		} else {
			left = BtreeTreeNode.of(new RecordSlice(this, 0, mid));
			right = BtreeTreeNode.of(
					new RecordSlice(this, mid, keyPos - mid),
					key,
					new RecordSlice(this, keyPos, size() - keyPos));
		}
		Tran.redir(adr, left);
		int rightAdr = Tran.refToInt(right);
		midKey = Record.of(
				new RecordSlice(midKey, 0, midKey.size() - 1),
				rightAdr);
		return new Split(adr, rightAdr, midKey);
	}

}
