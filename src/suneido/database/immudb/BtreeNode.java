/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Strings;

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

	void print(Writer w, int level) throws IOException {
		String indent = Strings.repeat("     ", level);
		w.append(indent).append(this instanceof BtreeLeafNode ? "Leaf" : "Tree").append("\n");
		for (int i = 0; i < size(); ++i) {
			Record slot = get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0) {
				int adr = (Integer) slot.get(slot.size() - 1);
				BtreeNode node = (level == 1)
					? Btree.leafNodeAt(adr) : Btree.treeNodeAt(adr);
				node.print(w, level - 1);
			}
		}
	}

}
