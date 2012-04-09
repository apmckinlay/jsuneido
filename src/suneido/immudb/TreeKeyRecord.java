/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * An ArrayRecord with a BtreeNode child reference.
 * The child reference may be null as a result of minimizing a dbnode key.
 */
class TreeKeyRecord extends ArrayRecord {
	BtreeNode child;

	TreeKeyRecord(ArrayList<ByteBuffer> bufs, TIntArrayList offs, TIntArrayList lens,
			BtreeNode child) {
		super(bufs, offs, lens);
		this.child = child;
	}

	@Override
	TreeKeyRecord minimize() {
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < size() - 1; ++i)
			rb.add("");
		rb.add(getRaw(size() - 1)); // keep child address
		return rb.treeKeyRecord(child);
	}

	@Override
	void freeze() {
		if (child != null)
			child.freeze();
	}

	@Override
	protected void append(StringBuilder sb, int i) {
		if (i == size() - 1 && child != null) {
			if (fieldLength(i) > 0) {
				super.append(sb, i);
				sb.append("/");
			}
			sb.append("REF");
		} else
			super.append(sb, i);
	}

}
