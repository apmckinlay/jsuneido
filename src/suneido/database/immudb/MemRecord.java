/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.List;

import suneido.language.Pack;
import suneido.util.IntArrayList;

import com.google.common.collect.Lists;

public class MemRecord extends Record {
	private final List<ByteBuffer> bufs = Lists.newArrayList();
	private final IntArrayList offs = new IntArrayList();
	private final IntArrayList lens = new IntArrayList();

	public MemRecord() {
	}

	/** add each of the fields of the record, NOT a nested record */
	public MemRecord add(Record r) {
		for (int i = 0; i < r.size(); ++i)
			add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	/** add a field of the record */
	public MemRecord add(Record r, int i) {
		add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	/** add a prefix of the fields of the record at buf,offset */
	public MemRecord add(ByteBuffer buf, int offset, int n) {
		for (int i = 0; i < n; ++i)
			add1(buf, DbRecord.fieldOffset(buf, offset, i), DbRecord.fieldLength(buf, offset, i));
		return this;
	}

	/** add an unsigned int
	 * needs to be unsigned so that intrefs compare > database offsets
	 */
	public MemRecord add(int n) {
		ByteBuffer buf = Pack.pack(n & 0xffffffffL);
		add1(buf, 0, buf.remaining());
		return this;
	}

	public MemRecord add(Object x) {
		ByteBuffer buf = Pack.pack(x);
		add1(buf, 0, buf.remaining());
		return this;
	}

	public MemRecord addNested(ByteBuffer buf, int offset) {
		add1(buf, offset, DbRecord.length(buf, offset));
		return this;
	}

	private void add1(ByteBuffer buf, int off, int len) {
		bufs.add(buf);
		offs.add(off);
		lens.add(len);
	}

	@Override
	public int size() {
		return bufs.size();
	}

	@Override
	public int fieldOffset(int i) {
		return offs.get(i);
	}

	@Override
	public int fieldLength(int i) {
		return lens.get(i);
	}

	@Override
	public ByteBuffer fieldBuffer(int i) {
		return bufs.get(i);
	}

}
