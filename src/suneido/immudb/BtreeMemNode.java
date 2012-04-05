/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * New and modified btree nodes.
 * Stored immutable nodes are in {@link BtreeDbNode}
 * Mutable within a transaction.
 * Changed to immutable via freeze before going into global database state.
 */
public class BtreeMemNode extends BtreeNode {
	private final Record rec;
	private final TByteArrayList index;
	private ArrayList<Record> added;
	private boolean immutable = false;
	/** set after node is stored */
	private int address;

	/** Create a new node, not based on an existing record */
	BtreeMemNode(int level) {
		this(level, null);
	}

	BtreeMemNode(BtreeDbNode node) {
		this(node.level, node.rec);
	}

	/** copy constructor, used when "modifying" an immutable node */
	private BtreeMemNode(BtreeMemNode node) {
		this(node.level, node.rec, new TByteArrayList(node.index), node.added);
	}

	BtreeMemNode(int level, Record rec) {
		this(level, rec, new TByteArrayList(), new ArrayList<Record>());
		if (rec != null)
			for (int i = 0; i < rec.size(); ++i)
				index.add((byte) i);
	}

	static BtreeMemNode from(int level, Record... data) {
		TByteArrayList index = new TByteArrayList();
		for (int i = 0; i < data.length; ++i)
			index.add((byte) (-i - 1));
		return new BtreeMemNode(level, null, index, Lists.newArrayList(data));
	}

	private BtreeMemNode(BtreeMemNode node, TByteArrayList index) {
		this(node.level, node.rec, index, node.added);
	}

	private BtreeMemNode(int level, Record rec, TByteArrayList index, ArrayList<Record> added) {
		super(level);
		this.rec = rec;
		this.index = index;
		this.added = added;
	}

	@Override
	BtreeMemNode with(Record key) {
		if (immutable)
			return new BtreeMemNode(this).with(key);
		else {
			int at = lowerBound(key);
			add(key);
			index.insert(at, (byte) -added.size());
			return this;
		}
	}

	private void add(Record key) {
		if (added.size() >= Byte.MAX_VALUE)
			cleanAdded();
		added.add(key);
	}

	private void cleanAdded() {
		ArrayList<Record> clean = Lists.newArrayList();
		for (int i = 0; i < index.size(); ++i) {
			int idx = index.get(i);
			if (idx < 0) {
				index.set(i, (byte) (-1 - clean.size()));
				clean.add(added.get(-idx - 1));
			}
		}
		assert clean.size() < Byte.MAX_VALUE;
		added = clean;
	}

	@Override
	BtreeNode without(int i) {
		if (immutable)
			return new BtreeMemNode(this).without(i);
		else {
			index.removeAt(i);
			return this;
		}
	}

	@Override
	BtreeNode without(int from, int to) {
		if (immutable)
			return new BtreeMemNode(this).without(from, to);
		else {
			index.remove(from, to - from);
			return this;
		}
	}

	@Override
	public BtreeNode withUpdate(int i, BtreeNode child) {
		Record key = get(i);
		if (key.childRef() == child)
			return this;
		else if (immutable)
			return new BtreeMemNode(this).withUpdate(i, child);
		else {
			Record newkey = new RecordBuilder()
					.addPrefix(key, key.size() - 1).addRef(child).build();
			add(newkey); // added is shared so can't update directly
			index.set(i, (byte) -added.size());
			return this;
		}
	}

	/** returns a new node containing a range of this node */
	@Override
	BtreeNode slice(int from, int to) {
		TByteArrayList index2 = new TByteArrayList(index.subList(from, to));
		return new BtreeMemNode(this, index2);
	}

	// called from BtreeDbNode
	static BtreeNode slice(BtreeDbNode node, int from, int to) {
		Preconditions.checkArgument(from < to && to <= node.size());
		TByteArrayList index = new TByteArrayList(to - from);
		for (int i = from; i < to; ++i)
			index.add((byte) i);
		return new BtreeMemNode(node.level, node.rec, index, new ArrayList<Record>());
	}

	@Override
	Record get(int i) {
		int idx = index.get(i);
		return idx >= 0
				? Record.from(rec.fieldBuffer(idx), rec.fieldOffset(idx))
				: added.get(-idx - 1);
	}

	@Override
	int size() {
		return index.size();
	}

	private int length(int i) {
		int idx = index.get(i);
		return idx >= 0
				? rec.fieldLength(idx)
				: added.get(-idx - 1).bufSize();
	}

	@Override
	BtreeMemNode minimizeLeftMost() {
		assert ! immutable;
		assert isTree();
		if (size() == 0)
			return this;
		Record key = get(0);
		if (isMinimalKey(key))
			return this;
		add(minimize(key));
		index.set(0, (byte) -added.size());
		return this;
	}

	/** NOTE: Assumes immutable nodes cannot have mutable children */
	@Override
	void freeze() {
		if (immutable)
			return;
		immutable = true;
		for (int i = 0; i < size(); ++i) {
			Record slot = get(i);
			BtreeNode child = (BtreeNode) slot.childRef();
			if (child != null)
				child.freeze();
		}
	}

	@Override
	boolean frozen() {
		return immutable;
	}

	// store -------------------------------------------------------------------

	@Override
	int store(Storage stor) {
		translate(stor);
		int adr = stor.alloc(length());
		ByteBuffer buf = stor.buffer(adr);
		pack(buf);
		return address = adr;
	}

	private void translate(Storage stor) {
		for (int i = 0; i < size(); ++i) {
			int j = index.get(i);
			if (j < 0) {
				Record r = added.get(-j - 1);
				if (r.childRef() instanceof BtreeMemNode)
					((BtreeMemNode) r.childRef()).store(stor);
			}
		}
	}

	int length() {
		int datasize = 0;
		for (int i = 0; i < size(); ++i)
			datasize += length(i);
		return ArrayRecord.length(size(), datasize);
	}

	void pack(ByteBuffer buf) {
		ArrayRecord.packHeader(buf, length(), getLengths(), 0);
		for (int i = size() - 1; i >= 0; --i)
			pack(buf, i);
	}

	private TIntArrayList getLengths() {
		TIntArrayList lens = new TIntArrayList(size());
		for (int i = 0; i < size(); ++i)
			lens.add(length(i));
		return lens;
	}

	private void pack(ByteBuffer buf, int i) {
		int idx = index.get(i);
		if (idx < 0)
			added.get(-idx - 1).pack(buf);
		else {
			int len = rec.fieldLength(idx);
			int off = rec.fieldOffset(idx);
			ByteBuffer src = rec.fieldBuffer(idx);
			for (int j = 0; j < len; ++j)
				buf.put(src.get(off + j));
		}
	}

	@Override
	String printName() {
		return "MemNode" + (immutable ? "(frozen)" : "") +
				(address == 0 ? "" : "@" + address);
	}

	@Override
	public int address() {
		return address;
	}

}
