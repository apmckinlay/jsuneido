/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.database.immudb.BtreeNode.Type.LEAF;
import static suneido.database.immudb.BtreeNode.Type.TREE;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;

import suneido.database.immudb.Btree.Split;

import com.google.common.base.Strings;

/**
 * Methods shared between {@link BtreeDbNode} and {@link BtreeMemNode}.
 * Can't inherit these methods because {@link BtreeDbNode} extends RecordBase
 * so BtreeNode must be an interface.
 */
public class BtreeNodeMethods {

	/**
	 * @param key The value to look for, without the trailing record address
	 * @return	The first key greater than or equal to the one specified
	 * 			or null if there isn't one.
	 */
	public static Record find(BtreeNode node, Record key) {
		int at = lowerBound(node, key.buf, key.offset);
		Record slot = node.get(at);
		if (node.type() == LEAF)
			return at < node.size() ? slot : null;
		else
			return slot.startsWith(key) ? slot : node.get(at - 1);
	}

	public static int lowerBound(BtreeNode node, ByteBuffer kbuf, int koff) {
		int first = 0;
		int len = node.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (Record.compare(node.fieldBuf(middle), node.fieldOffset(middle),
					kbuf, koff) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	public static Split split(Tran tran, BtreeNode node, Record key, int adr) {
		BtreeNode.Type type = node.type();
		BtreeNode left;
		BtreeNode right;
		Record splitKey;
		int keyPos = lowerBound(node, key.buf, key.offset);
		if (keyPos == node.size()) {
			// key is at end of node, just make new node
			right = new BtreeMemNode(tran, type).add(key);
			splitKey = key;
		} else {
			int mid = node.size() / 2;
			splitKey = node.get(mid);
			if (keyPos <= mid) {
				left = new BtreeMemNode(tran, type, node.buf())
						.add(node, 0, keyPos).add(key).add(node, keyPos, mid);
				right = new BtreeMemNode(tran, type, node.buf())
						.add(node, mid, node.size());
			} else {
				left = new BtreeMemNode(tran, type, node.buf())
						.add(node, 0, mid);
				right = new BtreeMemNode(tran, type, node.buf())
						.add(node, mid, keyPos).add(key).add(node, keyPos, node.size());
			}
			tran.redir(adr, left);
		}
		int splitKeySize = splitKey.size();
		if (type == TREE)
			--splitKeySize;
		int rightAdr = tran.refToInt(right);
		splitKey = new RecordBuilder().add(splitKey).add(rightAdr).build();
		return new Split(adr, rightAdr, splitKey);
	}

	public static String toString(BtreeNode node) {
		StringBuilder sb = new StringBuilder();
		sb.append(node.type());
		sb.append("[");
		for (int i = 0; i < node.size(); ++i)
			sb.append(node.get(i));
		sb.append("]");
		return sb.toString();
	}

	public static void print(Writer w, Tran tran, BtreeNode node, int level) throws IOException {
		String indent = Strings.repeat("     ", level);
		w.append(indent).append(node.type().toString()).append("\n");
		for (int i = 0; i < node.size(); ++i) {
			Record slot = node.get(i);
			w.append(indent).append(slot.toString()).append("\n");
			if (level > 0) {
				int adr = (Integer) slot.get(slot.size() - 1);
				print(w, tran,
						level == 1 ? Btree.leafNodeAt(tran, adr) : Btree.treeNodeAt(tran, adr),
						level - 1);
			}
		}
	}

	public static int persist(Tran tran, int ptr, int level) {
		if (! IntRefs.isIntRef(ptr))
			return ptr; // already in database
		BtreeMemNode node = (BtreeMemNode) ((level == 0)
				? Btree.leafNodeAt(tran, ptr) : Btree.treeNodeAt(tran, ptr));
		int adr = node.persist(level);
		tran.setAdr(ptr, adr);
		return adr;
	}

}
