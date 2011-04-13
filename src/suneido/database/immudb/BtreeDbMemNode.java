/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import gnu.trove.list.array.TByteArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Preconditions;

/**
 * When a BtreeDbNode is "modified" the result is a BtreeDbMemNode.
 * "old" data is retrieved from the DbRecord.
 * "new data is stored in the added array.
 * The added array is not sorted (new values are added at the end)
 * and it may contain old deleted values.
 * Since added is only appended to, it is safe to "share".
 */
@NotThreadSafe
public class BtreeDbMemNode extends BtreeStoreNode {
	private final DbRecord rec;
	private final TByteArrayList index;
	private final ArrayList<Record> added;

	public BtreeDbMemNode(BtreeDbNode node) {
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

	private BtreeDbMemNode(int level, DbRecord rec, TByteArrayList index, ArrayList<Record> added) {
		super(level);
		this.rec = rec;
		this.index = index;
		this.added = added;
		fix();
	}

	@Override
	public BtreeDbMemNode with(Record key) {
		assert added.size() < Byte.MAX_VALUE;
		int at = lowerBound(key);
		index.insert(at, (byte) (-1 - added.size()));
		added.add(key);
		return this;
	}

	@Override
	protected BtreeNode without(int i) {
		index.removeAt(i);
		return this;
	}

	@Override
	public BtreeNode slice(int from, int to) {
		TByteArrayList index2 = new TByteArrayList(index.subList(from, to));
		return new BtreeDbMemNode(this, index2);
	}

	// called from BtreeDbNode
	public static BtreeNode slice(BtreeDbNode node, int from, int to) {
		Preconditions.checkArgument(from < to && to <= node.size());
		TByteArrayList index = new TByteArrayList(to - from);
		for (int i = from; i < to; ++i)
			index.add((byte) i);
		return new BtreeDbMemNode(node, index);
	}

	/** at is relative to original node */
	@Override
	public BtreeNode sliceWith(int from, int to, int at, Record key) {
		return slice(from, to).with(key);
	}

	@Override
	public Record get(int i) {
		int idx = index.get(i);
		return idx >= 0
				? new DbRecord(rec.fieldBuffer(idx), rec.fieldOffset(idx))
				: added.get(-idx - 1);
	}

	@Override
	public int size() {
		return index.size();
	}

	@Override
	protected void translate(Tran tran) {
		for (int i = 0; i < index.size(); ++i)
			if (i < 0)
				added.set(-i - 1, translate(tran, added.get(-i - 1)));
	}

	@Override
	protected int length(int i) {
		int idx = index.get(i);
		return idx >= 0
				? rec.fieldLength(idx)
				: added.get(-idx - 1).length();
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

	/** ensure leftmost key of tree nodes is minimal */
	private void fix() {
		if (level == 0 || isEmpty())
			return;
		Record key = get(0);
		if (isMinimalKey(key))
			return;
		key = minimize(key);
		index.set(0, (byte) (-1 - added.size()));
		added.add(key);
	}

}
