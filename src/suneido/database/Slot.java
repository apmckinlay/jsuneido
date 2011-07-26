/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;

/**
 * Holds an index node "slot" while in memory. Comparisons are by key only. (not
 * address) Used with {@link Slots} Addresses (file offsets) are stored as int's
 * by aligning and shifting right. (See {@link Mmfile})
 */
class Slot implements suneido.Packable, Comparable<Slot> {
	final Record key;
	final long adr;

	Slot(Record key) {
		this.key = key;
		adr = -1;
	}

	Slot(Record key, long adr) {
		this.key = key;
		this.adr = adr;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Slot))
			return false;
		return 0 == compareTo((Slot) other);
	}

	@Override
	public int compareTo(Slot other) {
		return key.compareTo(other.key);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	int packSize() {
		return packSize(0);
	}

	@Override
	public int packSize(int nest) {
		return key.packSize(nest + 1) + (adr == -1 ? 0 : 4);
	}

	@Override
	public void pack(ByteBuffer buf) {
		key.pack(buf);
		if (adr != -1)
			buf.putInt(Mmfile.offsetToInt(adr));
	}

	/**
	 *
	 * @param buf
	 *            A ByteBuffer containing a packed Slot. <b>Note:</b> The limit
	 *            of the buffer must be correct.
	 * @return A new Slot containing the values from the buffer.
	 */
	static Slot unpack(ByteBuffer buf) {
		Record key = new Record(buf);
		if (buf.limit() <= key.bufSize())
			return new Slot(key);
		else {
			buf.position(key.bufSize());
			long adr = Mmfile.intToOffset(buf.getInt());
			return new Slot(key, adr);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(key.toString());
		sb.append(" ").append(adr);
		return sb.toString();
	}

	boolean isEmpty() {
		return key.isEmpty();
	}

	/**
	 * @return The address at the end of the key. (not from adr)
	 */
	long keyRecOff() {
		return key.getMmoffset(key.size() - 1);
	}

	int keyRecAdr() {
		return key.getInt(key.size() - 1);
	}

	Slot dup() {
		return key == Record.MINREC ? this : new Slot(key.dup(), adr);
	}
}
