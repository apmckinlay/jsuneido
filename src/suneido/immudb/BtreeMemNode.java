/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.immudb.Btree.Split;

import com.google.common.collect.Lists;

/**
 * New mutable nodes not derived from BtreeDbNode.
 */
@NotThreadSafe
class BtreeMemNode extends BtreeStorableNode {
	private final ArrayList<Record> data;

	BtreeMemNode(int level) {
		super(level);
		data = Lists.newArrayList();
	}

	BtreeMemNode(int level, Record... data) {
		super(level);
		this.data = Lists.newArrayList(data);
	}

	static BtreeNode newRoot(Tran tran, Split split) {
		Record minkey = minimalKey(split.key.size(), split.left);
		return new BtreeMemNode(split.level + 1, minkey, split.key);
	}

	@Override
	int size() {
		return data.size();
	}

	@Override
	Record get(int i) {
		return data.get(i);
	}

	@Override
	BtreeMemNode with(Record key) {
		int at = lowerBound(key);
		data.add(at, key);
		return this;
	}

	@Override
	protected BtreeNode without(int i) {
		data.remove(i);
		return this;
	}

	@Override
	BtreeNode without(int from, int to) {
		data.subList(from, to).clear();
		return this;
	}

	@Override
	BtreeNode slice(int from, int to) {
		checkArgument(from < to && to <= size());
		BtreeMemNode node = new BtreeMemNode(level);
		node.data.addAll(data.subList(from, to));
		return node;
	}

	@Override
	protected int length(int i) {
		return data.get(i).bufSize();
	}

	@Override
	public void pack(ByteBuffer buf, int i) {
		data.get(i).pack(buf);
	}

	@Override
	protected void translate(Tran tran) {
		for (int i = 0; i < size(); ++i)
			data.set(i, translate(tran, data.get(i)));
	}

	@Override
	void minimizeLeftMost() {
		assert isTree();
		data.set(0, minimize(data.get(0)));
	}

}
