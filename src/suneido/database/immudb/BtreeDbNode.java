/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * A {@link BtreeNode} wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a {@link BtreeMemNode}
 */
@Immutable
public class BtreeDbNode extends BtreeNode {
	private final DbRecord rec;

	public BtreeDbNode(int level, ByteBuffer buf) {
		super(level);
		rec = new DbRecord(buf, 0);
	}

	@Override
	public BtreeNode with(Record key) {
		return new BtreeMemNode(this).with(key);
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		return new DbRecord(rec.fieldBuffer(i), rec.fieldOffset(i));
	}

	@Override
	public BtreeNode without(Record key) {
		return new BtreeMemNode(this).without(key);
	}

	@Override
	public int size() {
		return rec.size();
	}

	@Override
	public int store(Tran tran) {
		return 0;
	}

}
