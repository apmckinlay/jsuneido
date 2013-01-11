/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import java.util.AbstractList;

import suneido.util.ByteBuf;

import com.google.common.base.Objects;

/**
 * Collection of {@link Slot}'s for a {@link Btree} node,
 * plus next and prev addresses (offsets in the database).
 * Next and prev are stored at the start of the buffer
 * followed by a {@link Record} holding the slots.
 * Addresses (file offsets) are stored as int's
 * by aligning and shifting right. (See {@link Mmfile})
 */
class Slots extends AbstractList<Slot> {
	private static final int NEXT_OFFSET = 0;
	private static final int PREV_OFFSET = 4;
	private static final int REC_OFFSET = 8;
	protected static final int BUFREC_SIZE = Btree.NODESIZE - REC_OFFSET;

	private final ByteBuf buf;
	private final Record rec;

	Slots(ByteBuf buf) {
		this(buf, Mode.OPEN);
	}
	Slots(ByteBuf buf, Mode mode) {
		this.buf = buf;
		if (mode == Mode.OPEN)
			rec = new Record(buf.slice(REC_OFFSET, BUFREC_SIZE));
		else { // mode == CREATE
			setNext(0);
			setPrev(0);
			rec = new Record(buf.slice(REC_OFFSET, BUFREC_SIZE), BUFREC_SIZE);
		}
	}

	@Override
	public boolean isEmpty() {
		return rec.isEmpty();
	}
	/**
	 * @return The number of slots currently stored.
	 */
	@Override
	public int size() {
		return rec.size();
	}

	Slot front() {
		return get(0);
	}
	Slot back() {
		return get(size() - 1);
	}
	@Override
	public Slot get(int i) {
		return Slot.unpack(rec.getRaw(i));
	}

	@Override
	public boolean add(Slot slot) {
		rec.add(slot);
		return true;
	}
	void add(Slots slots, int begin, int end) {
		for (int i = begin; i < end; ++i) {
			rec.add(slots.rec.getRaw(i));
		}
	}

	boolean insert(int i, Slot slot) {
		return rec.insert(i, slot);
	}

	@Override
	public Slot remove(int i) {
		rec.remove(i);
		return null;
	}
	void remove(int begin, int end) {
		rec.remove(begin, end);
	}
	void removeLast() {
		remove(size() - 1);
	}

	int remaining() {
		return rec.available();
	}

	long next() {
		return Mmfile.intToOffset(buf.getInt(NEXT_OFFSET));
	}
	long prev() {
		return Mmfile.intToOffset(buf.getInt(PREV_OFFSET));
	}
	void setNext(long value) {
		setBufNext(buf, value);
	}
	void setPrev(long value) {
		setBufPrev(buf, value);
	}

	/**
	 * Used to avoid instantiating a Slots object just to set next
	 *
	 * @param buf
	 * @param value
	 */
	static void setBufNext(ByteBuf buf, long value) {
		buf.putInt(NEXT_OFFSET, Mmfile.offsetToInt(value));
	}

	/**
	 * Used to avoid instantiating a Slots object just to set prev
	 *
	 * @param buf
	 * @param value
	 */
	static void setBufPrev(ByteBuf buf, long value) {
		buf.putInt(PREV_OFFSET, Mmfile.offsetToInt(value));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Slots))
			return false;
		return Objects.equal(rec, ((Slots) other).rec);
	}
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("next", next())
				.add("prev", prev())
				.addValue(rec)
				.toString();
	}

}
