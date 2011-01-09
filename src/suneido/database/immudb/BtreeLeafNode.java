/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.Util.lowerBound;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

/**
 * Stores one or more keys in sorted order.
 * Keys are records.
 * Keys have the data address as the last field.
 * Has next and prev pointers stored immediately following the record.
 * Pointers are long offsets into database file, stored as int using {@link IntLongs}
 */
@Immutable
public class BtreeLeafNode extends BtreeNode {
	public static final BtreeLeafNode EMPTY = new BtreeLeafNode();

	private BtreeLeafNode() {
		super();
	}

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
	 *
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

	Split split(Record key, int adr) {
		// TODO if key is at end of node, just make new node
		int keyPos = lowerBound(this, key);
		int mid = size() / 2;
		Record midKey = get(mid);
		BtreeLeafNode left;
		BtreeLeafNode right;
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
		int rightAdr = Tran.refToInt(right);
		midKey = Record.of(
				new RecordSlice(midKey, 0, midKey.size()),
				rightAdr);
		return new Split(adr, rightAdr, midKey);
	}

//	public int prev() {
//		return buf == emptyRecBuf ? 0 : buf.getInt(super.length());
//	}
//
//	public int next() {
//		return buf == emptyRecBuf ? 0 : buf.getInt(super.length() + 4);
//	}
//
//	@Override
//	public int length() {
//		return super.length() + 8;
//	}

}
