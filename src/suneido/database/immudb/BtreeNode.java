/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import suneido.database.immudb.Btree.Split;

import com.google.common.base.Strings;

public abstract class BtreeNode {
	public enum Type { LEAF, TREE };
	public final Type type;

	BtreeNode(Type type) {
		this.type = type;
	}

	public abstract int size();

	/** Inserts key in order */
	public abstract BtreeNode with(Record key);

	public abstract Record get(int i);

	public abstract Split split(Record key, int adr);

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	The first key greater than or equal to the one specified
	 * 			or null if there isn't one.
	 */
	public Record find(Record key) {
		int at = lowerBound(key.buf, key.offset);
		Record slot = get(at);
		if (type == Type.LEAF)
			return at < size() ? slot : null;
		else
			return slot.startsWith(key) ? slot : get(at - 1);
	}

	protected abstract int lowerBound(ByteBuffer kbuf, int koff);

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append("[");
		for (int i = 0; i < size(); ++i)
			sb.append(get(i));
		sb.append("]");
		return sb.toString();
	}

	void print(Writer w, int level) throws IOException {
		String indent = Strings.repeat("     ", level);
		w.append(indent).append(type.toString()).append("\n");
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
