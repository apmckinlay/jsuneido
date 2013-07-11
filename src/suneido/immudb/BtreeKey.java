/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedInts;

/**
 * A Btree leaf key. Holds the key record and the data record address.
 * Also used as the base for BtreeTreeKey.
 */
@Immutable
class BtreeKey implements Comparable<BtreeKey> {
	final static BtreeKey EMPTY = new BtreeKey(Record.EMPTY);
	final Record key;
	final int dataAdr;

	BtreeKey(Record key) {
		this(key, 0);
	}

	BtreeKey(Record key, int adr) {
		this.key = key;
		this.dataAdr = adr;
	}

	int adr() {
		return dataAdr;
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		if (that instanceof BtreeKey)
			return 0 == compareTo((BtreeKey) that);
		return false;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(BtreeKey other) {
		int cmp = key.compareTo(other.key);
		return (cmp != 0) ? cmp : UnsignedInts.compare(dataAdr, other.dataAdr);
	}

	boolean isEmptyKey() {
		for (int i = 0; i < key.size(); ++i)
			if (key.fieldLength(i) != 0)
				return false;
		return true;
	}

	boolean isMinimalKey() {
		return dataAdr == 0 && isEmptyKey();
	}

	BtreeKey minimize() {
		throw new UnsupportedOperationException();
	}

	int keySize() {
		return key.dataSize();
	}

	int packSize() {
		return Ints.BYTES + key.packSize();
	}

	void pack(ByteBuffer buf) {
		buf.putInt(dataAdr);
		key.pack(buf);
	}

	static BtreeKey unpack(ByteBuffer buf, int pos) {
		int adr = buf.getInt(pos);
		assert adr != 0;
		Record key = Record.from(buf, pos + Ints.BYTES);
		return new BtreeKey(key, adr);
	}

	/** overridden by BtreeTreeKey */
	void freeze() {
	}

	@Override
	public String toString() {
		return key.toString() +
				(dataAdr == 0 ? "" : "*" +
						(dataAdr == IntRefs.MAXADR ? "MAX" : dataAdr));
	}

}
