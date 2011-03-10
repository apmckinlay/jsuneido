/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * An btree node wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a BtreeMemNode
 */
@Immutable
public class BtreeDbNode extends BtreeNode {
	private final int level; // 0 means leaf
	private final Record rec;

	public BtreeDbNode(int level, ByteBuffer buf) {
		rec = new Record(buf, 0);
		this.level = level;
	}

	@Override
	public int level() {
		return level;
	}

	@Override
	public ByteBuffer buf() {
		return rec.buf;
	}

	@Override
	public BtreeNode with(Record key) {
		return new BtreeMemNode(this).with(key);
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		return new Record(rec.buf, rec.offset + fieldOffset(i));
	}

	@Override
	public ByteBuffer fieldBuf(int i) {
		return rec.buf;
	}

	@Override
	public int store(Tran tran) {
		return 0;
	}

	@Override
	public int size() {
		return rec.size();
	}

	@Override
	public int fieldOffset(int i) {
		return rec.fieldOffset(i);
	}

}
