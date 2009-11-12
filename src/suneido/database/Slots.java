package suneido.database;

import java.util.AbstractList;

import suneido.util.ByteBuf;

/**
 * Collection of {@link Slot}'s for a {@link Btree} node,
 * plus next and prev addresses (offsets in the database).
 * Next and prev are stored at the start of the buffer
 * followed by a {@link Record} holding the slots.
 * Addresses (file offsets) are stored as int's
 * by aligning and shifting right. (See {@link Mmfile})
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Slots extends AbstractList<Slot> {
	private final static int NEXT_OFFSET = 0;
	private final static int PREV_OFFSET = 4;
	private final static int REC_OFFSET = 8;
	protected final static int BUFREC_SIZE = Btree.NODESIZE - REC_OFFSET;

	private final ByteBuf buf;
	private final Record rec;

	public Slots(ByteBuf buf) {
		this(buf, Mode.OPEN);
	}
	public Slots(ByteBuf buf, Mode mode) {
		this.buf = buf;
		if (mode == Mode.OPEN)
			rec = new Record(buf.slice(REC_OFFSET));
		else { // mode == CREATE
			setNext(0);
			setPrev(0);
			rec = new Record(buf.slice(REC_OFFSET), BUFREC_SIZE);
		}
	}

	@Override
	public String toString() {
		return "Slots next " + next() + " prev " + prev() + " " + rec;
	}

	@Override
	public boolean isEmpty() {
		return rec.size() == 0;
	}
	/**
	 * @return The number of slots currently stored.
	 */
	@Override
	public int size() {
		return rec.size();
	}

	public Slot front() {
		return get(0);
	}
	public Slot back() {
		return get(size() - 1);
	}
	@Override
	public Slot get(int i) {
		return Slot.unpack(rec.getraw(i));
	}

	@Override
	public boolean add(Slot slot) {
		rec.add(slot);
		return true;
	}
	public void add(Slots slots, int begin, int end) {
		for (int i = begin; i < end; ++i) {
			rec.add(slots.rec.getraw(i));
		}
	}

	public boolean insert(int i, Slot slot) {
		return rec.insert(i, slot);
	}

	@Override
	public Slot remove(int i) {
		rec.remove(i);
		return null;
	}
	public void remove(int begin, int end) {
		rec.remove(begin, end);
	}
	public void removeLast() {
		remove(size() - 1);
	}

	public int remaining() {
		return rec.available();
	}

	public long next() {
		return Mmfile.intToOffset(buf.getInt(NEXT_OFFSET));
	}
	public long prev() {
		return Mmfile.intToOffset(buf.getInt(PREV_OFFSET));
	}
	public void setNext(long value) {
		setBufNext(buf, value);
	}
	public void setPrev(long value) {
		setBufPrev(buf, value);
	}

	/**
	 * Used to avoid instantiating a Slots object just to set next
	 *
	 * @param buf
	 * @param value
	 */
	public static void setBufNext(ByteBuf buf, long value) {
		buf.putInt(NEXT_OFFSET, Mmfile.offsetToInt(value));
	}

	/**
	 * Used to avoid instantiating a Slots object just to set prev
	 *
	 * @param buf
	 * @param value
	 */
	public static void setBufPrev(ByteBuf buf, long value) {
		buf.putInt(PREV_OFFSET, Mmfile.offsetToInt(value));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Slots))
			return false;
		Slots that = (Slots) other;
		return this.rec.equals(that.rec);
	}

}
