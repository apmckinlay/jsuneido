/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.util.Util.lowerBound;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Btree.Split;

/**
 * Stores one or more keys.
 * Keys are records.
 * Keys have the data address as the second last field
 * and pointer to child node as the last field.
 * The child node contains keys >= this key.
 * Pointers are long offsets into database file, stored as int using {@link IntLongs}
 */
@Immutable
public class BtreeTreeNode extends BtreeNode {

	public BtreeTreeNode(ByteBuffer buf) {
		super(buf);
	}

	private BtreeTreeNode(RecordBuilder rb) {
		this(rb.asByteBuffer());
	}

	public static BtreeTreeNode of(Record a, Record b) {
		return new BtreeTreeNode(new RecordBuilder().add(a).add(b));
	}

	public Record find(Record key) {
		int at = lowerBound(this, key) - 1;
		return get(at);
	}

	public static BtreeTreeNode newRoot(Split split) {
		RecordBuilder key1 = new RecordBuilder();
		for (int i = 0; i < split.key.size(); ++i)
			key1.add("");
		key1.add(split.left);

		Record key2 = Record.of(
			new RecordSlice(split.key, 0, split.key.size()),
			split.right);
		return BtreeTreeNode.of(key1.build(), key2);
	}

}
