/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;

/**
 * Abstract base class for store-able nodes {@link BtreeDbMemNode} and
 * {@link BtreeMemNode}.
 */
abstract class BtreeStorableNode extends BtreeNode {

	protected BtreeStorableNode(int level) {
		super(level);
	}

	@Override
	int store(Tran tran) {
		translate(tran);
		int adr = tran.stor.alloc(length());
		ByteBuffer buf = tran.stor.buffer(adr);
		pack(buf);
		return adr;
	}

	/** convert data record addresses and node pointers */
	protected abstract void translate(Tran tran);

	int length() {
		int datasize = 0;
		for (int i = 0; i < size(); ++i)
			datasize += length(i);
		return RecordBuilder.length(size(), datasize);
	}

	protected abstract int length(int i);

	void pack(ByteBuffer buf) {
		RecordBuilder.packHeader(buf, length(), getLengths());
		for (int i = size() - 1; i >= 0; --i)
			pack(buf, i);
	}

	protected TIntArrayList getLengths() {
		TIntArrayList lens = new TIntArrayList(size());
		for (int i = 0; i < size(); ++i)
			lens.add(length(i));
		return lens;
	}

	protected abstract void pack(ByteBuffer buf, int i);

	protected Record translate(Tran tran, Record rec) {
		boolean translate = false;

		int dref = dataref(rec);
		if (dref != 0) {
			dref = tran.redir(dref);
			if (IntRefs.isIntRef(dref)) {
				dref = tran.getAdr(dref);
				assert dref != 0;
				translate = true;
			}
		}

		int ptr = 0;
		if (level > 0) {
			int ptr1 = ptr = pointer(rec);
			if (! IntRefs.isIntRef(ptr1))
				ptr = tran.redir(ptr1);
			if (IntRefs.isIntRef(ptr))
				ptr = tran.getAdr(ptr);
			assert ptr != 0;
			assert ! IntRefs.isIntRef(ptr) : "pointer " + ptr1 + " => " + (ptr & 0xffffffffL);
			if (ptr1 != ptr)
				translate = true;
			//TODO if redirections are unique then we can remove this one
		}

		if (! translate)
			return rec;

		int prefix = rec.size() - (level > 0 ? 2 : 1);
		RecordBuilder rb = new RecordBuilder().addPrefix(rec, prefix);
		if (dref == 0)
			rb.add("");
		else
			rb.adduint(dref);
		if (level > 0)
			rb.adduint(ptr);
		return rb.build();
	}

	private int dataref(Record rec) {
		int size = rec.size();
		int i = size - (level > 0 ? 2 : 1);
		Object x = rec.get(i);
		if (x.equals(""))
			return 0;
		return ((Number) x).intValue();
	}

}
