/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkElementIndex;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

/**
 * A {@link BtreeNode} wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a {@link BtreeDbMemNode}
 * Immutable except for refs.
 */
class BtreeDbNode extends BtreeNode {
	final Record rec;
	private SoftReference<BtreeDbNode>[] refs = null; // cache child nodes

	BtreeDbNode(int level, ByteBuffer buf, int adr) {
		super(level);
		rec = Record.from(adr, buf, 0);
	}

	@Override
	BtreeKey get(int i) {
		checkElementIndex(i, rec.size());
		ByteBuffer buf = rec.fieldBuffer(i);
		int pos = rec.fieldOffset(i);
		return isLeaf()
				? BtreeKey.unpack(buf, pos)
				: BtreeTreeKey.unpack(buf, pos, ref(i));
	}

	protected BtreeDbNode ref(int i) {
		return refs == null || refs[i] == null ? null : refs[i].get();
	}

	@Override
	BtreeMemNode with(BtreeKey key) {
		return new BtreeMemNode(this).with(key);
	}

	@Override
	BtreeNode without(int i) {
		return new BtreeMemNode(this).without(i);
	}

	@Override
	public BtreeNode withUpdate(int i, BtreeNode child) {
		return new BtreeMemNode(this).withUpdate(i, child);
	}

	@Override
	int size() {
		return rec.size();
	}

	@Override
	BtreeDbNode store(Storage stor) {
		return this;
	}

	@Override
	public int address() {
		return rec.address();
	}

	@Override
	BtreeNode slice(int from, int to) {
		return BtreeMemNode.slice(this, from, to);
	}

	@Override
	BtreeNode without(int from, int to) {
		if (from == 0)
			return BtreeMemNode.slice(this, to, size());
		else if (to == size())
			return BtreeMemNode.slice(this, 0, from);
		else
			throw new IllegalArgumentException();
	}

	@Override
	BtreeNode minimizeLeftMost() {
		throw new UnsupportedOperationException();
	}

	@Override
	void freeze() {
	}

	@Override
	boolean frozen() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	BtreeDbNode childNode(Storage stor, int i) {
		BtreeDbNode ref = ref(i);
		if (ref != null)
			return ref;
		int childAdr = ((BtreeTreeKey) get(i)).childAddress();
		BtreeDbNode child = (BtreeDbNode) Btree.nodeAt(stor, level - 1, childAdr);
		if (refs == null)
			refs = new SoftReference[rec.size()];
		refs[i]  = new SoftReference<>(child); // cache
		return child;
	}

	@Override
	String printName() {
		return "DbNode @ " + address();
	}

}
