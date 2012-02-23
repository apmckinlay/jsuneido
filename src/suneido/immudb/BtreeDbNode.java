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

	BtreeDbNode(int level, ByteBuffer buf, int adr) {
		super(level);
		rec = Record.from(adr, buf, 0);
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
	int size() {
		return rec.size();
	}

	@Override
	int store(Tran tran) {
		throw new RuntimeException("shouldn't reach here");
	}

	@Override
	int store2(Storage stor) {
		throw new RuntimeException("shouldn't reach here");
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
	void minimizeLeftMost() {
		throw new UnsupportedOperationException();
	}

	@Override
	void freeze() {
	}

	@Override
	String printName() {
		return "DbNode @ " + address();
	}

}
