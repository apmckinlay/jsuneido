/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * A {@link BtreeNode} wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a {@link BtreeDbMemNode}
 */
@Immutable
class BtreeDbNode extends BtreeNode {
	final Record rec;

	BtreeDbNode(int level, ByteBuffer buf) {
		super(level);
		rec = new Record(buf, 0);
	}

	@Override
	Record get(int i) {
		Preconditions.checkElementIndex(i, rec.size());
		return new Record(rec.fieldBuffer(i), rec.fieldOffset(i));
	}

	@Override
	BtreeDbMemNode with(Record key) {
		return new BtreeDbMemNode(this).with(key);
	}

	@Override
	protected BtreeNode without(int i) {
		return new BtreeDbMemNode(this).without(i);
	}

	@Override
	int size() {
		return rec.size();
	}

	@Override
	int store(Tran tran) {
		throw new RuntimeException("shouldn't reach here");
	}

	@Override
	BtreeNode slice(int from, int to) {
		return BtreeDbMemNode.slice(this, from, to);
	}

	@Override
	BtreeNode without(int from, int to) {
		if (from == 0)
			return BtreeDbMemNode.slice(this, to, size());
		else if (to == size())
			return BtreeDbMemNode.slice(this, 0, from);
		else
			throw new IllegalArgumentException();
	}

	@Override
	void minimizeLeftMost() {
		throw new UnsupportedOperationException();
	}

}
