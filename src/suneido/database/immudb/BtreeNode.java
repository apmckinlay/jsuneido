/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

@Immutable
public abstract class BtreeNode extends RecordBase<Record> {

	protected BtreeNode() {
		super();
	}

	public BtreeNode(ByteBuffer buf) {
		super(buf, 0);
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		return new Record(buf, offset + getOffset(i));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this instanceof BtreeLeafNode ? "Leaf" : "Tree");
		sb.append("[");
		for (int i = 0; i < size(); ++i)
			sb.append(get(i));
		sb.append("]");
		return sb.toString();
	}

}
