package suneido.database;

import java.nio.ByteBuffer;

/**
 * Collection of {@link Slot}'s for a {@link Btree} node,
 * plus next and prev addresses (offsets in the database).
 * Next and prev are stored at the start of the buffer
 * followed by a {@link BufRecord} holding the slots.
 * @author Andrew McKinlay
 */
public class Slots {
	final private static int NEXT_OFFSET = 0;
	final private static int PREV_OFFSET = 8;
	final private static int REC_OFFSET = 16;
	final protected static int BUFREC_SIZE = Btree.NODESIZE - REC_OFFSET;
	enum Mode { INIT };
	
	private ByteBuffer buf;
	private BufRecord rec;
	
	public Slots(ByteBuffer buf) {
		this.buf = buf;
		buf.position(REC_OFFSET);
		rec = new BufRecord(buf.slice());
	}
	public Slots(ByteBuffer buf, Mode mode) {
		this.buf = buf;
		buf.position(REC_OFFSET);
		rec = new BufRecord(buf.slice(), BUFREC_SIZE);
	}
	
	public boolean empty() {
		return rec.size() == 0;
	}
	/**
	 * @return The number of slots currently stored.
	 */
	public int size() {
		return rec.size();
	}
	
	public Slot front() {
		return get(0);
	}
	public Slot back() {
		return get(size() - 1);
	}
	public Slot get(int i) {
		return Slot.unpack(rec.get(i));
	}
	
	public void add(Slot slot) {
		rec.add(slot);
	}
	public void add(Slots slots, int begin, int end) {
		for (int i = begin; i < end; ++i) {
			rec.add(slots.rec.get(i));
		}
	}
	
	public boolean insert(int i, Slot slot) {
		return rec.insert(i, slot);
	}
	
	public void erase(int i) {
	} //TODO 
	public void erase(Slot slot) {
	} //TODO
	public void erase(int begin, int end) {
	} //TODO
	
	public long next() {
		return buf.getLong(NEXT_OFFSET);
	}
	public long prev() {
		return buf.getLong(PREV_OFFSET);
	}
	public void setNext(long value) {
		buf.putLong(NEXT_OFFSET, value);
	}
	public void setPrev(long value) {
		buf.putLong(PREV_OFFSET, value);
	}

	/**
	 * Used to avoid instantiating a Slots object just to set next.
	 * @param buf
	 * @param value
	 */
	public static void setBufNext(ByteBuffer buf, long value) {
		buf.putLong(NEXT_OFFSET, value);
	}
	/**
	 * Used to avoid instantiating a Slots object just to set next.
	 * @param buf
	 * @param value
	 */
	public static void setBufPrev(ByteBuffer buf, long value) {
		buf.putLong(PREV_OFFSET, value);
	}
	
	/**
	 * Based on C++ STL code.
	 * @param slot
	 * @return The <u>first</u> position where slot could be inserted
	 * without changing the ordering.
	 */
	public int lower_bound(Slot slot) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (get(middle).compareTo(slot) < 0) {
				first = middle + 1;
				len = len - half - 1;
			}
			else
				len = half;
		}
		return first;
	}

	/**
	 * Based on C++ STL code.
	 * @param slot
	 * @return The <u>last</u> position where slot could be inserted
	 * without changing the ordering.
	 */
	public int upper_bound(Slot slot) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (slot.compareTo(get(middle)) < 0)
				len = half;
			else {
				first = middle + 1;
				len = len - half - 1;
			}
		}
		return first;
	}
}
