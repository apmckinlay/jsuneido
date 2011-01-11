/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.database.immudb.BtreeNode.Type.LEAF;
import static suneido.database.immudb.BtreeNode.Type.TREE;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * An btree node wrapping a ByteBuffer from the database.
 * "updating" a BtreeDbNode produces a BtreeMemNode
 * Extends RecordBase to avoid an extra layer of wrapping.
 */
@Immutable
public class BtreeDbNode extends RecordBase implements BtreeNode {
	private final Type type;

	public static BtreeDbNode leaf(ByteBuffer buf) {
		return new BtreeDbNode(LEAF, buf);
	}

	public static BtreeDbNode tree(ByteBuffer buf) {
		return new BtreeDbNode(TREE, buf);
	}

	private BtreeDbNode(Type type, ByteBuffer buf) {
		super(buf, 0);
		this.type = type;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public ByteBuffer buf() {
		return buf;
	}

	@Override
	public BtreeNode with(Record key) {
		return new BtreeMemNode(this).with(key);
	}

	@Override
	public Record get(int i) {
		if (i >= size())
			return Record.EMPTY;
		return new Record(buf, offset + fieldOffset(i));
	}

	@Override
	public Record find(Record key) {
		return BtreeNodeMethods.find(this, key);
	}

	@Override
	public Btree.Split split(Record key, int adr) {
		return BtreeNodeMethods.split(this, key, adr);
	}

	@Override
	public ByteBuffer fieldBuf(int i) {
		return buf;
	}

	@Override
	public String toString() {
		return BtreeNodeMethods.toString(this);
	}

}
