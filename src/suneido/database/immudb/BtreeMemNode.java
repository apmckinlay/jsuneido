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
	private final Tran tran;
	private final Type type;
	private final ByteBuffer buf;
	private final IntArrayList data = new IntArrayList();

	public BtreeMemNode(Tran tran, Type type) {
		this.tran = tran;
		this.type = type;
		buf = null;
	}

	public BtreeMemNode(Tran tran, Type type, ByteBuffer buf) {
		this.tran = tran;
		this.type = type;
		this.buf = buf;
	}

	public BtreeMemNode(Tran tran, BtreeDbNode node) {
		this.tran = tran;
		type = node.type();
		buf = node.buf;
		for (int i = 0; i < node.size(); ++i)
			data.add(node.fieldOffset(i));
	}

	public static BtreeMemNode emptyLeaf(Tran tran) {
		return new BtreeMemNode(tran, Type.LEAF);
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
			return (Record) tran.intToRef(x);
		else
			return new Record(buf, x);
	}

	public BtreeMemNode add(Record key) {
		data.add(tran.refToInt(key));
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
	public BtreeNode with(Tran tran, Record key) {
		int at = BtreeNodeMethods.lowerBound(this, key.buf, key.offset);
		data.add(at, tran.refToInt(key));
		return this;
	}

	public static BtreeNode newRoot(Tran tran, Split split) {
		RecordBuilder key1 = new RecordBuilder();
		for (int i = 0; i < split.key.size() - 1; ++i)
			key1.add("");
		key1.add(split.left);
		return new BtreeMemNode(tran, Type.TREE).add(key1.build()).add(split.key);
	}

	@Override
	public ByteBuffer fieldBuf(int i) {
		int x = data.get(i);
		if (IntRefs.isIntRef(x)) {
			Record r = (Record) tran.intToRef(x);
			return r.buf;
		} else
			return buf;
	}

	@Override
	public int fieldOffset(int i) {
		int x = data.get(i);
		if (IntRefs.isIntRef(x)) {
			Record r = (Record) tran.intToRef(x);
			return r.offset;
		} else
			return x;
	}

	@Override
	public Record find(Record key) {
		return BtreeNodeMethods.find(this, key);
	}

	@Override
	public Btree.Split split(Tran tran, Record key, int adr) {
		return BtreeNodeMethods.split(tran, this, key, adr);
	}

	@Override
	public String toString() {
		return BtreeNodeMethods.toString(this);
	}

	public int persist(int level) {
		RecordBuilder rb = builder(level);
		int adr = tran.mmf().alloc(rb.length());
		ByteBuffer buf = tran.mmf().buffer(adr);
		rb.toByteBuffer(buf);
		return adr;
	}

	private RecordBuilder builder(int level) {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < size(); ++i) {
			Record r = translate(i, level);
			if (r != null)
				rb.addNested(r.buf, r.offset);
			else
				rb.addNested(fieldBuf(i), fieldOffset(i));
		}
		return rb;
	}

	private Record translate(int i, int level) {
		ByteBuffer buf = fieldBuf(i);
		int offset = fieldOffset(i);
		int size = Record.size(buf, offset);
		boolean translate = false;

		int data = tran.redir(dataref(buf, offset, size, level));
		if (IntRefs.isIntRef(data)) {
			data = tran.getAdr(data);
			translate = true;
		}
		int ptr = 0;
		if (level > 0) {
			ptr = tran.redir(pointer(buf, offset, size));
			if (IntRefs.isIntRef(ptr)) {
				ptr = BtreeNodeMethods.persist(tran, ptr, level - 1);
				translate = true;
			}
		}
		if (! translate)
			return null;

		int prefix = size - (level > 0 ? 2 : 1);
		RecordBuilder rb = new RecordBuilder().add(buf, offset, prefix);
		rb.add(data);
		if (level > 0)
			rb.add(ptr);
		return rb.build();
	}

	private int pointer(ByteBuffer buf, int offset, int size) {
		return (int) ((Number) Record.get(buf, offset, size - 1)).longValue();
	}

	private int dataref(ByteBuffer buf, int offset, int size, int level) {
		int at = size - (level > 0 ? 2 : 1);
		Object x = Record.get(buf, offset, at);
		if (x.equals(""))
			return 1; // non-zero, non-intref
		return (int) ((Number) x).longValue();
	}

}
