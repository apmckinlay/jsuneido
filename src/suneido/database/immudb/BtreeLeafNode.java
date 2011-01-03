/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.Util.lowerBound;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * Stores one or more keys in sorted order.
 * Keys are records.
 * Keys have the data address as the last field.
 * Has next and prev pointers stored immediately following the record.
 * Pointers are long offsets into database file, stored as int using {@link IntLongs}
 */
@Immutable
public class BtreeLeafNode extends DbRecord {

	public BtreeLeafNode() {
		super();
	}

	public BtreeLeafNode(ByteBuffer buf) {
		super(buf);
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return DbRecord.EMPTY;
		return new DbRecord(buf, getOffset(i));
	}

	public BtreeLeafNode with(Record key) {
		int at = lowerBound(this, key);
		return withAt(key, at);
	}

	// TODO optimize - shouldn't need to create temp MemRecord
	private BtreeLeafNode withAt(Record key, int at) {
		MemRecord mr = new MemRecord();
		for (int i = 0; i < at; ++i)
			mr.add(get(i));
		mr.add(key);
		for (int i = at; i < size(); ++i)
			mr.add(get(i));
		int len = mr.length();
		ByteBuffer buf = ByteBuffer.allocate(len + 8);
		mr.addTo(buf);
		buf.putInt(prev());
		buf.putInt(next());
		return new BtreeLeafNode(buf);
	}

	public int prev() {
		return buf == emptyRecBuf ? 0 : buf.getInt(super.length());
	}

	public int next() {
		return buf == emptyRecBuf ? 0 : buf.getInt(super.length() + 4);
	}

	@Override
	public int length() {
		return super.length() + 8;
	}

}
