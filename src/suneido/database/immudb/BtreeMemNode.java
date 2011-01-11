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
public class BtreeMemNode implements BtreeNode {
	private final Type type;
	private final ByteBuffer buf;
	private final int[] data = new int[Btree.MAX_NODE_SIZE];
	private int size;

	public BtreeMemNode(Type type) {
		this.type = type;
		buf = null;
		size = 0;
	}

	public BtreeMemNode(Type type, ByteBuffer buf) {
		this.type = type;
		this.buf = buf;
		size = 0;
	}

	public BtreeMemNode(BtreeDbNode node) {
		type = node.type();
		buf = node.buf;
		size = node.size();
		for (int i = 0; i < size; ++i)
			data[i] = node.fieldOffset(i);
	}

	public static BtreeMemNode emptyLeaf() {
		return new BtreeMemNode(Type.LEAF);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public ByteBuffer buf() {
		return buf;
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

	public BtreeMemNode add(BtreeNode node, int from, int to) {
		if (node instanceof BtreeMemNode) {
			BtreeMemNode mnode = (BtreeMemNode) node;
			for (int i = from; i < to; ++i)
				data[size++] = mnode.data[i];
		} else {
			for (int i = from; i < to; ++i)
				data[size++] = node.fieldOffset(i);
		}
		return this;
	}

	@Override
	public BtreeNode with(Record key) {
		int at = BtreeNodeMethods.lowerBound(this, key.buf, key.offset);
		System.arraycopy(data, at, data, at + 1, size - at);
		data[at] = Tran.refToInt(key);
		++size;
		return this;
	}

	public static BtreeNode newRoot(Split split) {
		RecordBuilder key1 = new RecordBuilder();
		for (int i = 0; i < split.key.size() - 1; ++i)
			key1.add("");
		key1.add(split.left);
		return new BtreeMemNode(Type.TREE).add(key1.build()).add(split.key);
	}

	@Override
	public ByteBuffer fieldBuf(int i) {
		if (IntRefs.isIntRef(data[i])) {
			Record r = (Record) Tran.intToRef(data[i]);
			return r.buf;
		} else
			return buf;
	}

	@Override
	public int fieldOffset(int i) {
		if (IntRefs.isIntRef(data[i])) {
			Record r = (Record) Tran.intToRef(data[i]);
			return r.offset;
		} else
			return data[i];
	}

	@Override
	public Record find(Record key) {
		return BtreeNodeMethods.find(this, key);
	}

	@Override
	public Btree.Split split(Record key, int adr) {
		return BtreeNodeMethods.split(this, key, adr);
	}

	@Override
	public String toString() {
		return BtreeNodeMethods.toString(this);
	}

	public int persist(int level) {
		if (level > 0) {
			for (int i = 0; i < size(); ++i) {
				int ptr = pointer(i);
				if (IntRefs.isIntRef(ptr))
					updatePointer(i, BtreeNodeMethods.persist(ptr, level - 1));
			}
		}
		return persistRecord();
	}

	private int pointer(int i) {
		ByteBuffer buf = fieldBuf(i);
		int offset = fieldOffset(i);
		int size = Record.size(buf, offset);
		return (Integer) Record.get(buf, offset, size - 1);
	}

	private void updatePointer(int i, int ptr) {
		ByteBuffer buf = fieldBuf(i);
		int offset = fieldOffset(i);
		int size = Record.size(buf, offset);
		RecordBuilder rb = new RecordBuilder();
		rb.add(buf, offset, size - 1);
	}

	private int persistRecord() {
		// TODO persistRecord
		return 0;
	}

}
