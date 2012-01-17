/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TByteArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * When a BtreeDbNode is "modified" the result is a mutable BtreeDbMemNode.
 * "old" data is retrieved from the DbRecord.
 * "new" data is stored in the added array.
 * The added array is not sorted (new values are added at the end)
 * and it may contain old deleted values.
 * Since added is only appended to, it is safe to "share".
 */
@NotThreadSafe
class BtreeDbMemNode extends BtreeStorableNode {
	private final Record rec;
	private final TByteArrayList index;
	private ArrayList<Record> added;

	BtreeDbMemNode(BtreeDbNode node) {
		super(node.level);
		rec = node.rec;
		index = new TByteArrayList();
		for (int i = 0; i < node.size(); ++i)
			index.add((byte) i);
		added = new ArrayList<Record>();
	}

	private BtreeDbMemNode(BtreeDbNode node, TByteArrayList index) {
		this(node.level, node.rec, index, new ArrayList<Record>());
	}

	private BtreeDbMemNode(BtreeDbMemNode node, TByteArrayList index) {
		this(node.level, node.rec, index, node.added);
	}

	private BtreeDbMemNode(int level, Record rec, TByteArrayList index, ArrayList<Record> added) {
		super(level);
		this.rec = rec;
		this.index = index;
		this.added = added;
	}

	@Override
	BtreeDbMemNode with(Record key) {
		int at = lowerBound(key);
		add(key);
		index.insert(at, (byte) -added.size());
		return this;
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
	protected BtreeNode without(int i) {
		index.removeAt(i);
		return this;
	}

	@Override
	BtreeNode without(int from, int to) {
		index.remove(from, to - from);
		return this;
	}

	@Override
	BtreeNode slice(int from, int to) {
		TByteArrayList index2 = new TByteArrayList(index.subList(from, to));
		return new BtreeDbMemNode(this, index2);
	}

	// called from BtreeDbNode
	static BtreeNode slice(BtreeDbNode node, int from, int to) {
		Preconditions.checkArgument(from < to && to <= node.size());
		TByteArrayList index = new TByteArrayList(to - from);
		for (int i = from; i < to; ++i)
			index.add((byte) i);
		return new BtreeDbMemNode(node, index);
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

	@Override
	protected void translate(Tran tran) {
		for (int i = 0; i < size(); ++i) {
			int idx = index.get(i);
			if (idx < 0)
				added.set(-idx - 1, translate(tran, added.get(-idx - 1)));
		}
	}

	@Override
	protected int length(int i) {
		int idx = index.get(i);
		return idx >= 0
				? rec.fieldLength(idx)
				: added.get(-idx - 1).bufSize();
	}

	@Override
	protected void pack(ByteBuffer buf, int i) {
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
	void minimizeLeftMost() {
		assert isTree();
		Record key = get(0);
		if (isMinimalKey(key))
			return;
		add(minimize(key));
		index.set(0, (byte) -added.size());
	}

}
