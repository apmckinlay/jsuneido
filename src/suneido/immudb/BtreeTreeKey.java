/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import com.google.common.primitives.Ints;

/**
 * NOTE: Do not need to override compareTo
 * (i.e. can use inherited BtreeKey.compareTo)
 * because childAddress is not used in comparisons.
 */
class BtreeTreeKey extends BtreeKey {
	private final int childAdr;
	private BtreeNode child;

	BtreeTreeKey(Record key, int dataAddress, int childAddress) {
		this(key, dataAddress, childAddress, null);
	}

	BtreeTreeKey(Record key, int dataAdr, int childAdr, BtreeNode child) {
		super(key, dataAdr);
		this.child = child;
		this.childAdr = childAdr;
		assert childAdr == 0 || child == null || child.address() == childAdr;
	}

	int childAddress() {
		return childAdr != 0 ? childAdr : child.address();
	}

	BtreeNode child() {
		return child;
	}

	/** Used by BtreeMemNode childNode to cache */
	void setChild(BtreeNode child) {
		assert this.child == null;
		assert child != null;
		this.child = child;
		assert childAdr == 0 || child == null || child.address() == childAdr;
	}

	BtreeTreeKey withChild(BtreeNode child) {
		return new BtreeTreeKey(key, dataAdr, 0, child);
	}

	@Override
	BtreeKey minimize() {
		return new BtreeTreeKey(Record.EMPTY, 0, childAdr, child);
	}

	@Override
	int packSize() {
		return Ints.BYTES + super.packSize();
	}

	@Override
	void pack(ByteBuffer buf) {
		assert childAddress() != 0;
		buf.putInt(childAddress());
		super.pack(buf);
	}

	static BtreeTreeKey unpack(ByteBuffer buf, int pos, BtreeNode child) {
		int childAdr = buf.getInt(pos);
		assert childAdr != 0;
		int dataAdr = buf.getInt(pos + Ints.BYTES);
		Record key = Record.from(buf, pos + 2 * Ints.BYTES);
		assert dataAdr != 0 || key.isEmpty(); // could be minimal
		return new BtreeTreeKey(key, dataAdr, childAdr, child);
	}

	@Override
	public String toString() {
		return super.toString() +
				(childAdr == 0 ? "" : "^" + childAdr) +
				(child == null ? "" : "^REF");
	}

	@Override
	void freeze() {
		if (child != null)
			child.freeze();
	}

}
