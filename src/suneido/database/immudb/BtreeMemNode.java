/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

import com.google.common.base.Preconditions;

/**
 * New nodes not derived from BtreeDbNode.
 */
@Immutable
public class BtreeMemNode extends BtreeStoreNode {
	static final BtreeNode EMPTY_LEAF = new BtreeMemNode(0);
	private final Record data[];

	BtreeMemNode(int level) {
		super(level);
		data = new Record[0];
	}

	BtreeMemNode(int level, Record... data) {
		super(level);
		if (level > 0)
			// leftmost key in tree nodes must be minimal
			data[0] = minimize(data[0]);
		this.data = data;
	}

	public static BtreeNode newRoot(Tran tran, Split split) {
		MemRecord minkey = minimalKey(split.key.size(), split.left);
		return new BtreeMemNode(split.level + 1, minkey, split.key);
	}

	@Override
	public int size() {
		return data.length;
	}

	@Override
	public Record get(int i) {
		Preconditions.checkElementIndex(i, data.length);
		return data[i];
	}

	@Override
	public BtreeMemNode with(Record key) {
		Record[] data2 = Arrays.copyOf(data, data.length + 1);
		int at = lowerBound(key);
		if (at < data.length)
			System.arraycopy(data2, at, data2, at + 1, data.length - at);
		data2[at] = key;
		return new BtreeMemNode(level, data2);
	}

	@Override
	protected BtreeNode without(int i) {
		return new BtreeMemNode(level, copyWithout(data, i));
	}

	Record[] copyWithout(Record[] src, int i) {
		Record[] dst = new Record[src.length - 1];
		System.arraycopy(src, 0, dst, 0, i);
		System.arraycopy(src, i + 1, dst, i, src.length - i - 1);
		return dst;
	}

	@Override
	public BtreeNode slice(int from, int to) {
		Preconditions.checkArgument(from < to && to <= size());
		Record[] data2 = Arrays.copyOfRange(data, from, to);
		return new BtreeMemNode(level, data2);
	}

	@Override
	public BtreeNode sliceWith(int from, int to, int at, Record key) {
		Preconditions.checkArgument(from < to && to <= size());
		Preconditions.checkArgument(from <= at && at <= to,
				"from " + from + " to " + to + " at " + at);
		Record[] data2 = new Record[to - from + 1];
		int src = from;
		int dst = 0;
		while (src < at)
			data2[dst++] = data[src++];
		data2[dst++] = key;
		while (src < to)
			data2[dst++] = data[src++];
		return new BtreeMemNode(level, data2);
	}

	@Override
	protected int length(int i) {
		return data[i].length();
	}

	@Override
	public void pack(ByteBuffer buf, int i) {
		data[i].pack(buf);
	}

	@Override
	protected void translate(Tran tran) {
		for (int i = 0; i < data.length; ++i)
			data[i] = translate(tran, data[i]);
	}

}
