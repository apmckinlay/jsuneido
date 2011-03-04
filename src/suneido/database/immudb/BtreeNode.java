/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.Btree.Split;

/**
 * Common interface for {@link BtreeDbNode} and {@link BtreeMemNode}
 * Provides access to a list of keys in sorted order {@link RecordBase}
 * Keys are {@link Record}'s.
 * The final field on leaf keys is a pointer to the corresponding data record.
 * A tree keys is a leaf key plus a pointer to the child node.
 * Pointers are {@link MmapFile} int's
 */
public interface BtreeNode {
	public enum Type { LEAF, TREE };

	public Type type();

	public int size();

	public ByteBuffer buf();

	/** Inserts key in order */
	public BtreeNode with(Tran tran, Record key);

	public Record get(int i);

	public ByteBuffer fieldBuf(int i);

	public int fieldOffset(int i);

	public Record find(Record key);

	public Split split(Tran tran, Record key, int adr);

}
