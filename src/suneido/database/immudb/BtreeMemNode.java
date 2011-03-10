/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.Btree.Split;

/**
 * An in-memory mutable btree node.
 * "updating" a {@link BtreeDbNode} produces a BtreeMemNode
 * <p>
 * To avoid copying/allocation, the keys from the original BtreeMemNode
 * are stored as buffer and offset (rather than creating a Record wrapper).
 */
@NotThreadSafe
public class BtreeMemNode implements BtreeNode {
	private final int level;
	private final ByteBuffer buf;
	private final List<Object> data = new ArrayList<Object>();

	public BtreeMemNode(int level) {
		this(level, null);
	}

	public BtreeMemNode(int level, ByteBuffer buf) {
		this.level = level;
		this.buf = buf;
	}

	public BtreeMemNode(BtreeDbNode node) {
		level = node.level();
		buf = node.buf;
		for (int i = 0; i < node.size(); ++i)
			data.add(node.fieldOffset(i));
	}

	public static BtreeMemNode emptyLeaf() {
		return new BtreeMemNode(0);
	}

	@Override
	public int size() {
		return data.size();
	}

	@Override
	public int level() {
		return level;
	}

	@Override
	public ByteBuffer buf() {
		return buf;
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		Object x = data.get(i);
		if (x instanceof Record)
			return (Record) x;
		else
			return new Record(buf, (Integer) x);
	}

	public BtreeMemNode add(Record key) {
		data.add(key);
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
		data.add(at, key);
		return this;
	}

	public static BtreeNode newRoot(Tran tran, Split split) {
		RecordBuilder key1 = new RecordBuilder();
		for (int i = 0; i < split.key.size() - 1; ++i)
			key1.add("");
		key1.add(split.left);
		return new BtreeMemNode(split.level + 1).add(key1.build()).add(split.key);
	}

	@Override
	public ByteBuffer fieldBuf(int i) {
		Object x = data.get(i);
		return (x instanceof Record) ? ((Record) x).buf : buf;
	}

	@Override
	public int fieldOffset(int i) {
		Object x = data.get(i);
		return (x instanceof Record) ? ((Record) x).offset : (Integer) x;
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

	@Override
	public int store(Tran tran) {
		RecordBuilder rb = build(tran);
		int adr = tran.stor.alloc(rb.length());
		ByteBuffer buf = tran.stor.buffer(adr);
		rb.toByteBuffer(buf);
		return adr;
	}

	private RecordBuilder build(Tran tran) {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < size(); ++i) {
			Record r = translate(tran, i);
			if (r != null)
				rb.addNested(r.buf, r.offset);
			else
				rb.addNested(fieldBuf(i), fieldOffset(i));
		}
		return rb;
	}

	private Record translate(Tran tran, int i) {
		ByteBuffer buf = fieldBuf(i);
		int offset = fieldOffset(i);
		int size = Record.size(buf, offset);
		boolean translate = false;

		int data = dataref(buf, offset, size);
		if (data != 0) {
			data = tran.redir(data);
			if (IntRefs.isIntRef(data)) {
				data = tran.getAdr(data);
				assert data != 0;
				translate = true;
			}
		}

		int ptr = 0;
		if (level > 0) {
			int ptr1 = ptr = pointer(buf, offset, size);
			if (! IntRefs.isIntRef(ptr1))
				ptr = tran.redir(ptr1);
			ptr = tran.getAdr(ptr);
//System.out.println("pointer " + ptr1 + " translated to " + ptr);
			assert ! IntRefs.isIntRef(ptr);
			if (ptr1 != ptr)
				translate = true;
			//TODO if redirections are unique then we can remove this one
		}

		if (! translate)
			return null;

		int prefix = size - (level > 0 ? 2 : 1);
		RecordBuilder rb = new RecordBuilder().add(buf, offset, prefix);
		if (data == 0)
			rb.add("");
		else
			rb.add(data);
		if (level > 0)
			rb.add(ptr);
		return rb.build();
	}

	private int dataref(ByteBuffer buf, int offset, int size) {
		int at = size - (level > 0 ? 2 : 1);
		Object x = Record.get(buf, offset, at);
		if (x.equals(""))
			return 0;
		return ((Number) x).intValue();
	}

	private int pointer(ByteBuffer buf, int offset, int size) {
		return ((Number) Record.get(buf, offset, size - 1)).intValue();
	}

}
