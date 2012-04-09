/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;

/**
 * A {@link BtreeNode} wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a {@link BtreeDbMemNode}
 * Immutable except for refs.
 */
class BtreeDbNode extends BtreeNode {
	final Record rec;
	final SoftReference<BtreeDbNode>[] refs; // cache child nodes

	@SuppressWarnings("unchecked")
	BtreeDbNode(int level, ByteBuffer buf, int adr) {
		super(level);
		rec = Record.from(adr, buf, 0);
		refs = new SoftReference[rec.size()];
	}

	BtreeDbNode(int level, ByteBuffer buf, int adr, SoftReference<BtreeDbNode>[] refs) {
		super(level);
		rec = Record.from(adr, buf, 0);
		this.refs = refs;
	}

	@Override
	Record get(int i) {
		Preconditions.checkElementIndex(i, rec.size());
		return Record.from(rec.fieldBuffer(i), rec.fieldOffset(i));
	}

	@Override
	BtreeMemNode with(Record key) {
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

	TreeKeyRecord minimize(int idx) {
		Record key = get(idx);
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < key.size() - 1; ++i)
			rb.add("");
		rb.add(key.getRaw(key.size() - 1)); // keep the child address
		BtreeDbNode child = (refs[idx] != null && refs[idx].get() != null)
			? refs[idx].get()
			: null;
		return rb.treeKeyRecord(child);

	}

	@Override
	void freeze() {
	}

	@Override
	boolean frozen() {
		return true;
	}

	@Override
	BtreeDbNode childNode(Storage stor, int i) {
		if (refs[i] != null) {
			BtreeDbNode ref = refs[i].get();
			if (ref != null)
				return ref;
		}
		BtreeDbNode child = (BtreeDbNode) Btree.nodeAt(stor, level - 1, adr(get(i)));
		refs[i]  = new SoftReference<BtreeDbNode>(child); // cache
		return child;
	}

	@Override
	String printName() {
		return "DbNode @ " + address();
	}

}
