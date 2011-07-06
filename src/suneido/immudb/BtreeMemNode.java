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
public class BtreeMemNode extends BtreeStoreNode {
	private final ArrayList<Record> data;

	BtreeMemNode(int level) {
		super(level);
		data = Lists.newArrayList();
	}

	BtreeMemNode(int level, Record... data) {
		super(level);
		this.data = Lists.newArrayList(data);
		fix();
	}

	public static BtreeNode newRoot(Tran tran, Split split) {
		Record minkey = minimalKey(split.key.size(), split.left);
		return new BtreeMemNode(split.level + 1, minkey, split.key);
	}

	@Override
	public int size() {
		return data.size();
	}

	@Override
	public Record get(int i) {
		return data.get(i);
	}

	@Override
	public BtreeMemNode with(Record key) {
		int at = lowerBound(key);
		data.add(at, key);
		fix();
		return this;
	}

	@Override
	protected BtreeNode without(int i) {
		data.remove(i);
		fix();
		return this;
	}

	@Override
	public BtreeNode without(int from, int to) {
		data.subList(from, to).clear();
		fix();
		return this;
	}

	@Override
	public BtreeNode slice(int from, int to) {
		checkArgument(from < to && to <= size());
		BtreeMemNode node = new BtreeMemNode(level);
		node.data.addAll(data.subList(from, to));
		node.fix();
		return node;
	}

	@Override
	protected int length(int i) {
		return data.get(i).length();
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

	private void fix() {
		if (level > 0)
			// leftmost key in tree nodes must be minimal
			data.set(0, minimize(data.get(0)));
	}

}
