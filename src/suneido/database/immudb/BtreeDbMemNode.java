/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * When a BtreeDbNode is "modified" the result is a BtreeDbMemNode.
 * "old" data is retrieved from the DbRecord.
 * "new data is stored in the added array.
 * The added array is not sorted (new values are added at the end)
 * and it may contain old deleted values.
 */
@Immutable
public class BtreeDbMemNode extends BtreeStoreNode {
	private static final Record[] NO_RECORDS = new Record[0];
	private final DbRecord rec;
	private final byte[] index;
	private Record[] added; // not final because it may be modified by fix

	private BtreeDbMemNode(BtreeDbNode node, byte[] index, Record[] added,
			boolean sharedAdded) {
		this(node.level, node.rec, index, added, sharedAdded);
	}

	private BtreeDbMemNode(BtreeDbMemNode node, byte[] index, Record[] added,
			boolean sharedAdded) {
		this(node.level, node.rec, index, added, sharedAdded);
	}

	private BtreeDbMemNode(int level, DbRecord rec, byte[] index, Record[] added,
			boolean sharedAdded) {
		super(level);
		this.rec = rec;
		this.index = index;
		this.added = added;
		fix(sharedAdded);
	}

	/** ensure leftmost key of tree nodes is minimal */
	private void fix(boolean sharedAdded) {
		if (level == 0 || isEmpty())
			return;
		Record key = get(0);
		if (isMinimalKey(key))
			return;
		key = minimize(key);
		int idx = index[0];
		if (idx < 0) {
			if (sharedAdded)
				added = added.clone();
			added[-idx - 1] = key;
		} else {
			idx = added.length;
			index[0] = (byte) (-1 - idx);
			added = Arrays.copyOf(added, added.length + 1);
			added[idx] = key;
		}
	}

	// called from BtreeDbNode
	static BtreeDbMemNode with(BtreeDbNode node, Record key) {
		int at = node.lowerBound(key);
		byte[] index = new byte[node.size() + 1];
		Record[] added = new Record[] { key };
		int i = 0;
		for (; i < at; ++i)
			index[i] = (byte) i;
		index[i++] = -1;
		for (; i < index.length; ++i)
			index[i] = (byte) (i - 1);
		return new BtreeDbMemNode(node, index, added, false);
	}

	@Override
	public BtreeDbMemNode with(Record key) {
		int at = lowerBound(key);
		byte[] index2 = Arrays.copyOf(index, index.length + 1);
		System.arraycopy(index2, at, index2, at + 1, index2.length - at - 1);
		index2[at] = (byte) (-1 - added.length);
		Record[] added2 = Arrays.copyOf(added, added.length + 1);
		added2[added.length] = key;
		return new BtreeDbMemNode(this, index2, added2, false);
	}

	// called from BtreeDbNode
	static BtreeNode without(BtreeDbNode node, int at) {
		byte[] index = new byte[node.size() - 1];
		int i = 0;
		for (; i < at; ++i)
			index[i] = (byte) i;
		for (; i < index.length; ++i)
			index[i] = (byte) (i + 1);
		return new BtreeDbMemNode(node, index, NO_RECORDS, true);
	}

	@Override
	protected BtreeNode without(int i) {
		Preconditions.checkElementIndex(i, size());
		if (size() == 1)
			return BtreeNode.emptyNode(level);
		// don't bother removing from added
		return new BtreeDbMemNode(this, copyWithout(index, i), added, true);
	}

	byte[] copyWithout(byte[] src, int i) {
		byte[] dst = new byte[src.length - 1];
		System.arraycopy(src, 0, dst, 0, i);
		System.arraycopy(src, i + 1, dst, i, src.length - i - 1);
		return dst;
	}

	@Override
	public BtreeNode slice(int from, int to) {
		Preconditions.checkArgument(from < to && to <= size());
		byte[] index2 = Arrays.copyOfRange(index, from, to);
		// don't bother updating added
		return new BtreeDbMemNode(this, index2, added, true);
	}

	// called from BtreeDbNode
	public static BtreeNode slice(BtreeDbNode node, int from, int to) {
		Preconditions.checkArgument(from < to && to <= node.size(),
				"from " + from + " to " + to + " size " + node.size());
		byte[] index = new byte[to - from];
		int src = from;
		int dst = 0;
		while (src < to)
			index[dst++] = (byte) src++;
		return new BtreeDbMemNode(node, index, NO_RECORDS, true);
	}

	/** at is relative to original node */
	@Override
	public BtreeNode sliceWith(int from, int to, int at, Record key) {
		Preconditions.checkArgument(from < to && to <= size());
		Preconditions.checkArgument(from <= at && at <= to);
		byte[] index2 = copyWith(index, from, to, at, -1 - added.length);
		Record[] added2 = Arrays.copyOf(added, added.length + 1);
		added2[added.length] = key;
		return new BtreeDbMemNode(this, index2, added2, false);
	}

	public static byte[] copyWith(byte[] index, int from, int to, int at, int add) {
		byte[] index2 = new byte[to - from + 1];
		int src = from;
		int dst = 0;
		while (src < at)
			index2[dst++] = index[src++];
		index2[dst++] = (byte) add;
		while (src < to)
			index2[dst++] = index[src++];
		return index2;
	}

	// called from BtreeDbNode
	/** at is relative to original node */
	public static BtreeNode sliceWith(BtreeDbNode node,
			int from, int to, int at, Record key) {
		Preconditions.checkArgument(from < to && to <= node.size());
		Preconditions.checkArgument(from <= at && at <= to);
		byte[] index = copyWith(from, to, at);
		Record[] added = new Record[] { key };
		return new BtreeDbMemNode(node, index, added, false);
	}

	public static byte[] copyWith(int from, int to, int at) {
		byte[] index = new byte[to - from + 1];
		int src = from;
		int dst = 0;
		while (src < at)
			index[dst++] = (byte) src++;
		index[dst++] = -1;
		while (src < to)
			index[dst++] = (byte) src++;
		return index;
	}

	@Override
	public Record get(int i) {
		Preconditions.checkElementIndex(i, index.length);
		int idx = index[i];
		return idx >= 0
				? new DbRecord(rec.fieldBuffer(idx), rec.fieldOffset(idx))
				: added[-idx - 1];
	}

	@Override
	public int size() {
		return index.length;
	}

	@Override
	protected void translate(Tran tran) {
		for (int i : index)
			if (i < 0)
				added[-i - 1] = translate(tran, added[-i - 1]);
	}

	@Override
	protected int length(int i) {
		int idx = index[i];
		return idx >= 0
				? rec.fieldLength(idx)
				: added[-idx - 1].length();
	}

	@Override
	protected void pack(ByteBuffer buf, int i) {
		int idx = index[i];
		if (idx < 0)
			added[-idx - 1].pack(buf);
		else {
			int len = rec.fieldLength(idx);
			int off = rec.fieldOffset(idx);
			ByteBuffer src = rec.fieldBuffer(idx);
			for (int j = 0; j < len; ++j)
				buf.put(src.get(off + j));
		}
	}

}
