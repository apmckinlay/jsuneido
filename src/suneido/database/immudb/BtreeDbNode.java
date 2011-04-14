/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * A {@link BtreeNode} wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a {@link BtreeDbMemNode}
 */
@Immutable
public class BtreeDbNode extends BtreeNode {
	final DbRecord rec;

	BtreeDbNode(int level, ByteBuffer buf) {
		super(level);
		rec = new DbRecord(buf, 0);
	}

	@Override
	public Record get(int i) {
		Preconditions.checkElementIndex(i, rec.size());
		return new DbRecord(rec.fieldBuffer(i), rec.fieldOffset(i));
	}

	@Override
	public BtreeDbMemNode with(Record key) {
		return new BtreeDbMemNode(this).with(key);
	}

	@Override
	protected BtreeNode without(int i) {
		return new BtreeDbMemNode(this).without(i);
	}

	@Override
	public int size() {
		return rec.size();
	}

	@Override
	public int store(Tran tran) {
		throw new RuntimeException("shouldn't reach here");
	}

	@Override
	public BtreeNode slice(int from, int to) {
		return BtreeDbMemNode.slice(this, from, to);
	}

	@Override
	public BtreeNode without(int from, int to) {
		if (from == 0)
			return BtreeDbMemNode.slice(this, to, size());
		else if (to == size())
			return BtreeDbMemNode.slice(this, 0, from);
		else
			throw new IllegalArgumentException();
	}

}
