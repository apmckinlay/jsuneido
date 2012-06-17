/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * New and modified btree nodes.
 * Stored immutable nodes are in {@link BtreeDbNode}
 * Mutable within a transaction.
 * Changed to immutable via freeze before going into global database state.
 */
class BtreeMemNode extends BtreeNode {
	/** almost final, but cleared by freeze if not needed */
	private BtreeDbNode dbnode;
	private final TByteArrayList index;
	/** almost final, but rebuilt by without(from, to) */
	private List<BtreeKey> added;
	private boolean immutable = false;
	/** set after node is stored */
	private int address;

	/** Create a new node, not based on an existing record */
	BtreeMemNode(int level) {
		this(level, null, new TByteArrayList(), new ArrayList<BtreeKey>());
	}

	BtreeMemNode(BtreeDbNode node) {
		this(node.level, node, dbnodeIndex(node.size()), new ArrayList<BtreeKey>());
	}

	private static TByteArrayList dbnodeIndex(int n) {
		TByteArrayList index = new TByteArrayList(n);
		for (int i = 0; i < n; ++i)
			index.add((byte) i);
		return index;
	}

	/** copy constructor, used when "modifying" an immutable node */
	private BtreeMemNode(BtreeMemNode node) {
		this(node.level, node.dbnode, new TByteArrayList(node.index), node);
	}

	/** used by Btree split and newRoot */
	static BtreeMemNode from(int level, BtreeKey... data) {
		TByteArrayList index = new TByteArrayList(data.length);
		for (int i = 0; i < data.length; ++i)
			index.add((byte) (-i - 1));
		return new BtreeMemNode(level, null, index, Lists.newArrayList(data));
	}

	private static boolean haveDbnodeRefs(TByteArrayList index) {
		for (int i = 0; i < index.size(); ++i)
			if (index.get(i) >= 0)
				return true;
		return false;
	}

	private BtreeMemNode(int level, BtreeDbNode dbnode,
			TByteArrayList index, BtreeMemNode node) {
		this(level, dbnode, index, copyAdded(index, node.added));
	}

	/** copies just the referenced elements of node.added */
	private static List<BtreeKey> copyAdded(TByteArrayList index, List<BtreeKey> added) {
		List<BtreeKey> copy = new ArrayList<BtreeKey>(countAdded(index));
		for (int i = 0; i < index.size(); ++i) {
			int idx = index.get(i);
			if (idx < 0) {
				copy.add(added.get(-idx - 1));
				index.set(i, (byte) -copy.size());
			}
		}
		return copy;
	}

	private static int countAdded(TByteArrayList index) {
		int nadded = 0;
		for (int i = 0; i < index.size(); ++i)
			if (index.get(i) < 0)
				++nadded;
		return nadded;
	}

	/** added must not be shared */
	private BtreeMemNode(int level, BtreeDbNode dbnode,
			TByteArrayList index, List<BtreeKey> added) {
		super(level);
		this.dbnode = haveDbnodeRefs(index) ? dbnode : null;
		this.index = index;
		this.added = added;
	}

	//--------------------------------------------------------------------------

	@Override
	BtreeMemNode with(BtreeKey key) {
		if (immutable)
			return new BtreeMemNode(this).with(key);
		else {
			int at = lowerBound(key);
			index.insert(at, add(key));
			return this;
		}
	}

	/** @return the value to put in the index list */
	private byte add(BtreeKey key) {
		assert ! immutable;
		assert isLeaf() || key instanceof BtreeTreeKey;
		// reuse free (null) added entries
		int i = added.indexOf(null);
		if (i != -1) {
			added.set(i, key);
			return (byte) -(i + 1);
		}
		added.add(key);
		assert added.size() < Byte.MAX_VALUE;
		return (byte) -added.size();
	}

	@Override
	BtreeNode without(int i) {
		if (immutable)
			return new BtreeMemNode(this).without(i);
		else {
			int idx = index.get(i);
			if (idx < 0)
				added.set(-idx - 1, null);
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
			added = copyAdded(index, added);
			return this;
		}
	}

	@Override
	public BtreeNode withUpdate(int i, BtreeNode child) {
		if (ref(i) == child)
			return this;
		else if (immutable)
			return new BtreeMemNode(this).withUpdate(i, child);
		else {
			update(i, ((BtreeTreeKey) get(i)).withChild(child));
			return this;
		}
	}

	private BtreeNode ref(int i) {
		int idx = index.get(i);
		if (idx < 0)
			return ((BtreeTreeKey) get(i)).child();
		else
			return dbnode.ref(idx);
	}

	private void update(int i, BtreeKey key) {
		int idx = index.get(i);
		if (idx < 0)
			added.set(-idx - 1, key);
		else
			index.set(i, add(key));
	}

	/** returns a new node containing a range of this node */
	@Override
	BtreeNode slice(int from, int to) {
		return new BtreeMemNode(level, dbnode,
				new TByteArrayList(index.subList(from, to)), this);
	}

	// called from BtreeDbNode
	static BtreeNode slice(BtreeDbNode node, int from, int to) {
		Preconditions.checkArgument(from < to && to <= node.size());
		TByteArrayList index = new TByteArrayList(to - from);
		for (int i = from; i < to; ++i)
			index.add((byte) i);
		return new BtreeMemNode(node.level, node, index, new ArrayList<BtreeKey>());
	}

	@Override
	BtreeKey get(int i) {
		int idx = index.get(i);
		return idx >= 0
				? dbnode.get(idx)
				: added.get(-idx - 1);
	}

	@Override
	int size() {
		return index.size();
	}

	@Override
	BtreeMemNode minimizeLeftMost() {
		assert ! immutable;
		assert isTree();
		if (size() == 0)
			return this;
		BtreeKey key = get(0);
		if (key.isMinimalKey())
			return this;
		key = key.minimize();
		update(0, key);
		return this;
	}

	/** NOTE: Assumes immutable nodes never have mutable children */
	@Override
	void freeze() {
		if (immutable)
			return;
		immutable = true;
		if (! haveDbnodeRefs(index))
			dbnode = null;
		if (isLeaf())
			return;
		for (int i = 0; i < size(); ++i) {
			byte idx = index.get(i);
			if (idx < 0) {
				added.get(-idx - 1).freeze();
			}
		}
	}

	@Override
	boolean frozen() {
		return immutable;
	}

	// store -------------------------------------------------------------------

	@Override
	BtreeDbNode store(Storage stor) {
		if (isTree())
			storeChildren(stor);
		address = stor.alloc(length());
		ByteBuffer buf = stor.buffer(address);
		pack(buf);
		BtreeDbNode node = new BtreeDbNode(level, buf, address);
		assert node.address() == address;
		return node;
	}

	private void storeChildren(Storage stor) {
		for (int i = 0; i < size(); ++i) {
			byte idx = index.get(i);
			if (idx < 0) {
				BtreeTreeKey key = (BtreeTreeKey) added.get(-idx - 1);
				if (key.child() != null)
					key.child().store(stor); // recursive
			}
		}
	}

	int length() {
		int datasize = 0;
		for (int i = 0; i < size(); ++i)
			datasize += length(i);
		return ArrayRecord.length(size(), datasize);
	}

	private int length(int i) {
		int idx = index.get(i);
		return idx >= 0
				? dbnode.rec.fieldLength(idx)
				: added.get(-idx - 1).packSize();
	}

	void pack(ByteBuffer buf) {
		ArrayRecord.packHeader(buf, length(), getLengths());
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
			int len = dbnode.rec.fieldLength(idx);
			int off = dbnode.rec.fieldOffset(idx);
			ByteBuffer src = dbnode.rec.fieldBuffer(idx);
			for (int j = 0; j < len; ++j)
				buf.put(src.get(off + j));
		}
	}

	@Override
	BtreeNode childNode(Storage stor, int i) {
		int idx = index.get(i);
		if (idx >= 0)
			return dbnode.childNode(stor, idx);
		BtreeTreeKey key = (BtreeTreeKey) added.get(-idx - 1);
		if (key.child() == null) {
			BtreeNode node = Btree.nodeAt(stor, level - 1, key.childAddress());
			key.setChild(node); // cache
		}
		return key.child();
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
