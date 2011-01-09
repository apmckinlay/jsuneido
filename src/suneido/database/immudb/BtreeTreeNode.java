/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.Util.lowerBound;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

/**
 * Keys are {@link BtreeLeafNode} keys plus a pointer to a child node as the last field.
 * The child node contains keys >= this key.
 * The nodes on the left "edge" of the tree (including the root)
 * have an initial empty key (other than the child pointer)
 * which points to keys less than the first "real" key.
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

	// identical to BtreeLeafNode.split except for adding splitKey address
	public Split split(Record key, int adr) {
		BtreeTreeNode left;
		BtreeTreeNode right;
		Record splitKey;
		int keyPos = lowerBound(this, key);
		if (keyPos == size()) {
			// key is at end of node, just make new node
			right = BtreeTreeNode.of(key);
			splitKey = key;
		} else {
			int mid = size() / 2;
			splitKey = get(mid);
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
		}
		int rightAdr = Tran.refToInt(right);
		splitKey = Record.of(
				new RecordSlice(splitKey, 0, splitKey.size() - 1),
				rightAdr);
		return new Split(adr, rightAdr, splitKey);
	}

}
