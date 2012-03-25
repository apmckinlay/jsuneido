/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

@Immutable
public class RecordPrefix extends Record {
	private final Record rec;
	private final int len;

	RecordPrefix(Record rec, int len) {
		assert len < rec.size();
		this.rec = rec;
		this.len = len;
	}

	@Override
	public int size() {
		return len;
	}

	@Override
	ByteBuffer fieldBuffer(int i) {
		return rec.fieldBuffer(i);
	}

	@Override
	int fieldLength(int i) {
		return rec.fieldLength(i);
	}

	@Override
	int fieldOffset(int i) {
		return rec.fieldOffset(i);
	}

	@Override
	public ByteBuffer getBuffer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int packSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pack(ByteBuffer buf) {
		throw new UnsupportedOperationException();
	}

}
