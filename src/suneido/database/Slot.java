/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.nio.ByteBuffer;

/**
 * Holds an index node "slot" while in memory.
 * Comparisons are by key only. (not address)
 * Used with {@link Slots}
 * Addresses (file offsets) are stored as int's
 * by aligning and shifting right. (See {@link Mmfile})
 */
public class Slot implements suneido.Packable, Comparable<Slot> {
	public final Record key;
	public final long adr;

	public Slot(Record key) {
		this.key = key;
		adr = -1;
	}

	public Slot(Record key, long adr) {
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

	public int compareTo(Slot other) {
		return key.compareTo(other.key);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	public int packSize() {
		return packSize(0);
	}
	public int packSize(int nest) {
		return key.packSize(nest + 1) + (adr == -1 ? 0 : 4);
	}
	public void pack(ByteBuffer buf) {
		key.pack(buf);
		if (adr != -1)
			buf.putInt(Mmfile.offsetToInt(adr));
	}

	/**
	 *
	 * @param buf A ByteBuffer containing a packed Slot.
	 * 		<b>Note:</b> The limit of the buffer must be correct.
	 * @return A new Slot containing the values from the buffer.
	 */
	public static Slot unpack(ByteBuffer buf) {
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

	public boolean isEmpty() {
		return key.isEmpty();
	}

	/**
	 * @return The address at the end of the key. (not from adr)
	 */
	public long keyadr() {
		return key.getMmoffset(key.size() - 1);
	}

	public Slot dup() {
		return key == Record.MINREC ? this : new Slot(key.dup(), adr);
	}
}
