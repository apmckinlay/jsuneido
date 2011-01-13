/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.Btree.Split;
import suneido.util.IntArrayList;

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
	private final IntArrayList data = new IntArrayList();

	public BtreeMemNode(Type type) {
		this.type = type;
		buf = null;
	}

	public BtreeMemNode(Type type, ByteBuffer buf) {
		this.type = type;
		this.buf = buf;
	}

	public BtreeMemNode(BtreeDbNode node) {
		type = node.type();
		buf = node.buf;
		for (int i = 0; i < node.size(); ++i)
			data.add(node.fieldOffset(i));
	}

	public static BtreeMemNode emptyLeaf() {
		return new BtreeMemNode(Type.LEAF);
	}

	@Override
	public int size() {
		return data.size();
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
		if (i >= size())
			return Record.EMPTY;
		int x = data.get(i);
		if (IntRefs.isIntRef(x))
			return (Record) Tran.intToRef(x);
		else
			return new Record(buf, x);
	}

	public BtreeMemNode add(Record key) {
		data.add(Tran.refToInt(key));
		return this;
	}

	public BtreeMemNode add(BtreeNode node, int from, int to) {
		if (node instanceof BtreeMemNode) {
			BtreeMemNode mnode = (BtreeMemNode) node;
			for (int i = from; i < to; ++i)
				data.add(mnode.data.get(i));
		} else {
			for (int i = from; i < to; ++i)
				data.add(node.fieldOffset(i));
		}
		return this;
	}

	@Override
	public BtreeNode with(Record key) {
		int at = BtreeNodeMethods.lowerBound(this, key.buf, key.offset);
		data.add(at, Tran.refToInt(key));
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
		int x = data.get(i);
		if (IntRefs.isIntRef(x)) {
			Record r = (Record) Tran.intToRef(x);
			return r.buf;
		} else
			return buf;
	}

	@Override
	public int fieldOffset(int i) {
		int x = data.get(i);
		if (IntRefs.isIntRef(x)) {
			Record r = (Record) Tran.intToRef(x);
			return r.offset;
		} else
			return x;
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
System.out.println("BEFORE " + this);
		if (level > 0) { // tree
			for (int i = 0; i < size(); ++i) {
				int ptr = Tran.redir(pointer(i));
				if (IntRefs.isIntRef(ptr))
					updatePointer(i, BtreeNodeMethods.persist(ptr, level - 1));
			}
System.out.println("UPDATED " + this);
		}
		return persistRecord();
	}

	private int pointer(int i) {
		ByteBuffer buf = fieldBuf(i);
		int offset = fieldOffset(i);
		int size = Record.size(buf, offset);
		return (Integer) Record.get(buf, offset, size - 1);
	}

	void updatePointer(int i, int ptr) {
System.out.println("updatePointer " + i + " to " + Integer.toHexString(ptr));
		ByteBuffer buf = fieldBuf(i);
		int offset = fieldOffset(i);
System.out.println("   " + new Record(buf, offset));
		int size = Record.size(buf, offset);
		Record r = new RecordBuilder().add(buf, offset, size - 1).add(ptr).build();
System.out.println("=> " + r);
		data.set(i, Tran.refToInt(r));
	}

	private int persistRecord() {
		RecordBuilder rb = builder();
		int adr = Tran.mmf().alloc(rb.length());
		rb.toByteBuffer(Tran.mmf().buffer(adr));
System.out.println("persistRecord => " + new Record(Tran.mmf().buffer(adr), 0));
		return adr;
	}

	RecordBuilder builder() {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < size(); ++i)
			rb.addNested(fieldBuf(i), fieldOffset(i));
		return rb;
	}

}
