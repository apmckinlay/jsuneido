/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import suneido.language.Pack;

class RecordBuilder extends ArrayRecord
		implements suneido.intfc.database.RecordBuilder {

	RecordBuilder() {
		super(new ArrayList<ByteBuffer>(), new TIntArrayList(), new TIntArrayList());
	}

	/** add a field of the record */
	RecordBuilder add(Record r, int i) {
		if (i < r.size())
			add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		else
			addMin();
		return this;
	}

	/** add a prefix of the fields of the record */
	RecordBuilder addPrefix(Record rec, int prefixLength) {
		for (int i = 0; i < prefixLength; ++i)
			add(rec, i);
		return this;
	}

	RecordBuilder addFields(Record rec, int... fields) {
		for (int f : fields)
			add(rec, f);
		return this;
	}

	@Override
	public RecordBuilder add(int n) {
		ByteBuffer buf = Pack.packLong(n);
		add1(buf, 0, buf.remaining());
		return this;
	}

	/** add an unsigned int
	 * needs to be unsigned so that intrefs compare > database offsets
	 */
	public RecordBuilder adduint(int n) {
		ByteBuffer buf = Pack.packLong(n & 0xffffffffL);
		add1(buf, 0, buf.remaining());
		return this;
	}

	@Override
	public RecordBuilder add(Object x) {
		ByteBuffer buf = Pack.pack(x);
		add1(buf, 0, buf.remaining());
		return this;
	}

	@Override
	public RecordBuilder addAll(suneido.intfc.database.Record rec) {
		Record r = (Record) rec;
		for (int i = 0; i < r.size(); ++i)
			add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	@Override
	public RecordBuilder add(ByteBuffer buf) {
		add1(buf, buf.position(), buf.remaining());
		return this;
	}

	@Override
	public RecordBuilder addMin() {
		return add(suneido.intfc.database.Record.MIN_FIELD);
	}

	@Override
	public RecordBuilder addMax() {
		return add(suneido.intfc.database.Record.MAX_FIELD);
	}

	private void add1(ByteBuffer buf, int off, int len) {
		bufs.add(buf);
		offs.add(off);
		lens.add(len);
	}

	@Override
	public RecordBuilder truncate(int n) {
		for (int i = bufs.size() - 1; i >= n; --i) {
			bufs.remove(i);
			offs.removeAt(i);
			lens.removeAt(i);
		}
		return this;
	}

	@Override
	public Record build() {
		assert offs.size() == bufs.size();
		assert lens.size() == bufs.size();
		return this;
	}

}
