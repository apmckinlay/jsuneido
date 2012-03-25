/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

public class ProjectRecord extends Record {
	private final Record rec;
	private final int[] fldnums;

	ProjectRecord(Record rec, int... fldnums) {
		this.rec = rec;
		this.fldnums = fldnums;
	}

	@Override
	public int size() {
		return fldnums.length;
	}

	@Override
	ByteBuffer fieldBuffer(int i) {
		return i < rec.size() ? rec.fieldBuffer(fldnums[i]) : Record.EMPTY_BUF;
	}

	@Override
	int fieldLength(int i) {
		return i < rec.size() ? rec.fieldLength(fldnums[i]) : 0;
	}

	@Override
	int fieldOffset(int i) {
		return i < rec.size() ? rec.fieldOffset(fldnums[i]) : 0;
	}

	Record record() {
		return rec;
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
