/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.Btree.Split;
import suneido.util.IntArrayList;

import com.google.common.collect.Lists;

/**
 * An in-memory mutable {@link BtreeNode}.
 * "updating" a {@link BtreeDbNode} produces a BtreeMemNode
 */
@NotThreadSafe
public class BtreeMemNode extends BtreeNode {
	List<Record> data = Lists.newArrayList();

	public BtreeMemNode(int level) {
		super(level);
	}

	public BtreeMemNode(BtreeDbNode node) {
		super(node.level());
		add(node, 0, node.size());
	}

	public static BtreeMemNode emptyLeaf() {
		return new BtreeMemNode(0);
	}

	@Override
	public int size() {
		return data.size();
	}

	@Override
	public Record get(int i) {
		return i < size() ? data.get(i) : Record.EMPTY;
	}

	public BtreeMemNode add(Record key) {
		data.add(key);
		return this;
	}

	/** add a range of keys from another node */
	public BtreeMemNode add(BtreeNode node, int from, int to) {
		for (int i = from; i < to; ++i)
			data.add(node.get(i));
		return this;
	}

	@Override
	public BtreeNode with(Record key) {
		int at = lowerBound(key);
		data.add(at, key);
		return this;
	}

	public static BtreeNode newRoot(Tran tran, Split split) {
		MemRecord key1 = new MemRecord();
		for (int i = 0; i < split.key.size() - 1; ++i)
			key1.add("");
		key1.add(split.left);
		return new BtreeMemNode(split.level + 1).add(key1).add(split.key);
	}

	public int length() {
		int datasize = 0;
		for (int i = 0; i < size(); ++i)
			datasize += get(i).length();
		return MemRecord.length(size(), datasize);
	}

	@Override
	public int store(Tran tran) {
		for (int i = 0; i < size(); ++i)
			translate(tran, i);

		int adr = tran.stor.alloc(length());
		ByteBuffer buf = tran.stor.buffer(adr);
		pack(buf);
		return adr;
	}

	public void pack(ByteBuffer buf) {
		MemRecord.packHeader(buf, length(), getLengths());
		for (int i = size() - 1; i >= 0; --i)
			data.get(i).pack(buf);
	}

	private IntArrayList getLengths() {
		IntArrayList lens = new IntArrayList(size());
		for (int i = 0; i < size(); ++i)
			lens.add(get(i).length());
		return lens;
	}

	private void translate(Tran tran, int i) {
		Record rec = data.get(i);
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
			return ;

		int prefix = rec.size() - (level > 0 ? 2 : 1);
		MemRecord r = new MemRecord().addPrefix(rec, prefix);
		if (dref == 0)
			r.add("");
		else
			r.add(dref);
		if (level > 0)
			r.add(ptr);
		data.set(i, r);
	}

	private int dataref(Record rec) {
		int size = rec.size();
		int i = size - (level > 0 ? 2 : 1);
		Object x = rec.get(i);
		if (x.equals(""))
			return 0;
		return ((Number) x).intValue();
	}

	private int pointer(Record rec) {
		return ((Number) rec.get(rec.size() - 1)).intValue();
	}

}
